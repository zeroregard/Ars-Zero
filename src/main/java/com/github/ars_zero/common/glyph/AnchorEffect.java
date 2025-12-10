package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.config.ServerConfig;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.common.block.MultiphaseSpellTurretTile;
import com.github.ars_zero.common.item.AbstractMultiphaseHandheldDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellPhase;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.TileCaster;
import com.hollingsworth.arsnouveau.common.block.BasicSpellTurret;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * AnchorEffect - Works with Temporal Context Form to maintain relative position of entities.
 * 
 * When used with Temporal Context Form in the TICK phase, this effect keeps the entity
 * locked to the same position on the player's screen, following their look direction.
 */
public class AnchorEffect extends AbstractEffect {
    
    public static final String ID = "anchor_effect";
    public static final AnchorEffect INSTANCE = new AnchorEffect();

    public AnchorEffect() {
        super(ArsZero.prefix(ID), "Anchor");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;
        if (!(world instanceof ServerLevel serverLevel)) return;
        
        ArsZero.LOGGER.info("[AnchorEffect] onResolveEntity: shooter type: {}, caster type: {}", 
            shooter != null ? shooter.getClass().getSimpleName() : "null",
            spellContext.getCaster() != null ? spellContext.getCaster().getClass().getSimpleName() : "null");
        
        MultiPhaseCastContext castContext = null;
        Player player = null;
        Vec3 casterPos = null;
        float casterYaw = 0;
        float casterPitch = 0;
        
        if (spellContext.getCaster() instanceof TileCaster tileCaster) {
            BlockEntity tile = tileCaster.getTile();
            ArsZero.LOGGER.info("[AnchorEffect] TileCaster detected, tile type: {}", 
                tile != null ? tile.getClass().getSimpleName() : "null");
            if (tile instanceof MultiphaseSpellTurretTile turretTile) {
                castContext = turretTile.getCastContext();
                ArsZero.LOGGER.info("[AnchorEffect] Turret tile found, castContext: {}, beginResults: {}", 
                    castContext != null, castContext != null ? castContext.beginResults.size() : 0);
                if (castContext != null && castContext.playerId != null) {
                    player = serverLevel.getServer().getPlayerList().getPlayer(castContext.playerId);
                    ArsZero.LOGGER.info("[AnchorEffect] Player lookup: playerId: {}, found: {}", 
                        castContext.playerId, player != null);
                }
                BlockPos tilePos = turretTile.getBlockPos();
                casterPos = Vec3.atCenterOf(tilePos);
                Direction facing = turretTile.getBlockState().getValue(BasicSpellTurret.FACING);
                casterYaw = directionToYaw(facing);
                casterPitch = directionToPitch(facing);
                ArsZero.LOGGER.info("[AnchorEffect] Turret position: {}, facing: {}, yaw: {}, pitch: {}", 
                    tilePos, facing, casterYaw, casterPitch);
            } else {
                ArsZero.LOGGER.warn("[AnchorEffect] Tile is not MultiphaseSpellTurretTile: {}", 
                    tile != null ? tile.getClass().getName() : "null");
            }
        } else if (shooter instanceof Player playerCaster) {
            ArsZero.LOGGER.info("[AnchorEffect] Player caster detected");
            player = playerCaster;
            ItemStack casterTool = spellContext.getCasterTool();
            castContext = AbstractMultiphaseHandheldDevice.findContextByStack(player, casterTool);
            casterPos = player.getEyePosition(1.0f);
            casterYaw = player.getYRot();
            casterPitch = player.getXRot();
            ArsZero.LOGGER.info("[AnchorEffect] Player context: {}, beginResults: {}", 
                castContext != null, castContext != null ? castContext.beginResults.size() : 0);
        } else {
            ArsZero.LOGGER.warn("[AnchorEffect] Caster is neither TileCaster nor Player: {}", 
                spellContext.getCaster() != null ? spellContext.getCaster().getClass().getName() : "null");
        }
        
        if (castContext == null) {
            ArsZero.LOGGER.warn("[AnchorEffect] No cast context found, returning");
            return;
        }
        
        if (castContext.beginResults.isEmpty()) {
            ArsZero.LOGGER.warn("[AnchorEffect] Cast context found but beginResults is empty");
            return;
        }
        
        ArsZero.LOGGER.info("[AnchorEffect] Processing {} beginResults with casterPos: {}, yaw: {}, pitch: {}", 
            castContext.beginResults.size(), casterPos, casterYaw, casterPitch);
        
        for (SpellResult beginResult : castContext.beginResults) {
            Entity target = beginResult.targetEntity;
            ArsZero.LOGGER.info("[AnchorEffect] onResolveEntity: Processing result - target: {}, blockGroup: {}, relativeOffset: {}, hitResult type: {}", 
                target != null ? target.getClass().getSimpleName() : "null",
                beginResult.blockGroup != null ? beginResult.blockGroup.getClass().getSimpleName() : "null",
                beginResult.relativeOffset != null,
                beginResult.hitResult != null ? beginResult.hitResult.getType().name() : "null");

            BlockGroupEntity blockGroup = null;
            if (target instanceof BlockGroupEntity bg) {
                blockGroup = bg;
                ArsZero.LOGGER.info("[AnchorEffect] onResolveEntity: Found blockGroup in targetEntity");
            } else if (beginResult.blockGroup != null) {
                blockGroup = beginResult.blockGroup;
                ArsZero.LOGGER.info("[AnchorEffect] onResolveEntity: Found blockGroup in beginResult.blockGroup");
            } else if (beginResult.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof BlockGroupEntity bg) {
                blockGroup = bg;
                ArsZero.LOGGER.info("[AnchorEffect] onResolveEntity: Found blockGroup in hitResult EntityHitResult");
            }
            
            if (blockGroup != null) {
                ArsZero.LOGGER.info("[AnchorEffect] onResolveEntity: Found blockGroup, adding lifespan");
                blockGroup.addLifespan(1);
                target = blockGroup;
            }
            
            if (target == null || !target.isAlive()) {
                ArsZero.LOGGER.debug("[AnchorEffect] Target is null or not alive, skipping");
                continue;
            }
            
            if (beginResult.relativeOffset == null) {
                ArsZero.LOGGER.debug("[AnchorEffect] relativeOffset is null, skipping");
                continue;
            }
            
            if (player != null && target instanceof Player targetPlayer) {
                if (!canAnchorPlayer(player, targetPlayer, serverLevel)) {
                    ArsZero.LOGGER.debug("[AnchorEffect] Cannot anchor player, skipping");
                    continue;
                }
                
                if (!areInSameChunk(player, targetPlayer)) {
                    ArsZero.LOGGER.debug("[AnchorEffect] Players not in same chunk, restoring physics");
                    restoreEntityPhysics(castContext);
                    continue;
                }
            }
            
            Vec3 newPosition = beginResult.transformLocalToWorld(
                casterYaw, 
                casterPitch, 
                casterPos,
                castContext.distanceMultiplier
            );
            
            ArsZero.LOGGER.info("[AnchorEffect] Calculated newPosition: {}, canMove: {}", 
                newPosition, newPosition != null && canMoveToPosition(newPosition, world));
            
            if (newPosition != null && canMoveToPosition(newPosition, world)) {
                if (target instanceof ServerPlayer targetPlayer) {
                    targetPlayer.teleportTo(newPosition.x, newPosition.y, newPosition.z);
                    targetPlayer.setDeltaMovement(Vec3.ZERO);
                    targetPlayer.setNoGravity(true);
                    ArsZero.LOGGER.info("[AnchorEffect] Teleported ServerPlayer to {}", newPosition);
                } else {
                    target.setPos(newPosition.x, newPosition.y, newPosition.z);
                    target.setDeltaMovement(Vec3.ZERO);
                    target.setNoGravity(true);
                    ArsZero.LOGGER.info("[AnchorEffect] Moved entity to {}", newPosition);
                    
                    if (target instanceof BaseVoxelEntity voxel) {
                        voxel.freezePhysics();
                    }
                }
            } else {
                ArsZero.LOGGER.warn("[AnchorEffect] Cannot move to position: newPosition={}, canMove={}", 
                    newPosition, newPosition != null && canMoveToPosition(newPosition, world));
            }
        }
    }
    
    private static float directionToYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0f;
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            case EAST -> -90.0f;
            default -> 0.0f;
        };
    }

    private static float directionToPitch(Direction facing) {
        return switch (facing) {
            case UP -> -90.0f;
            case DOWN -> 90.0f;
            default -> 0.0f;
        };
    }
    
    private static boolean canAnchorPlayer(Player caster, Player target, ServerLevel level) {
        if (caster == target) {
            return true;
        }
        
        if (ServerConfig.ALLOW_NON_OP_ANCHOR_ON_PLAYERS.get()) {
            return canInteractWithPlayer(caster, target, level);
        }
        
        if (caster instanceof ServerPlayer serverCaster) {
            return serverCaster.hasPermissions(2);
        }
        
        return false;
    }
    
    private static boolean canInteractWithPlayer(Player caster, Player target, ServerLevel level) {
        if (!(caster instanceof ServerPlayer serverCaster) || !(target instanceof ServerPlayer serverTarget)) {
            return true;
        }
        
        try {
            Class<?> ftbChunksClass = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            Object api = ftbChunksClass.getMethod("api").invoke(null);
            Object manager = api.getClass().getMethod("getManager").invoke(api);
            Object claimedChunk = manager.getClass().getMethod("getChunk", ServerLevel.class, int.class, int.class)
                    .invoke(manager, level, target.chunkPosition().x, target.chunkPosition().z);
            
            if (claimedChunk != null) {
                Object team = claimedChunk.getClass().getMethod("getTeam").invoke(claimedChunk);
                if (team != null) {
                    Object teamId = team.getClass().getMethod("getId").invoke(team);
                    net.minecraft.world.scores.Team casterTeam = serverCaster.getTeam();
                    if (casterTeam != null) {
                        String casterTeamId = casterTeam.getName();
                        return teamId.equals(casterTeamId);
                    }
                    return false;
                }
            }
        } catch (Exception e) {
        }
        
        return true;
    }
    
    private static boolean areInSameChunk(Player caster, Player target) {
        if (!(caster.level() instanceof ServerLevel) || !(target.level() instanceof ServerLevel)) {
            return true;
        }
        
        if (caster.level() != target.level()) {
            return false;
        }
        
        ChunkPos casterChunk = caster.chunkPosition();
        ChunkPos targetChunk = target.chunkPosition();
        
        return casterChunk.equals(targetChunk);
    }
    
    private static boolean canMoveToPosition(Vec3 targetPos, Level world) {
        BlockPos blockPos = BlockPos.containing(targetPos);
        
        return !world.getBlockState(blockPos).blocksMotion();
    }
    
    public static void restoreEntityPhysics(MultiPhaseCastContext context) {
        if (context == null || context.beginResults.isEmpty()) {
            return;
        }
        
        Player player = null;
        if (context.playerId != null && context.beginResults.get(0).casterPosition != null) {
            if (context.beginResults.get(0).targetEntity != null && context.beginResults.get(0).targetEntity.level() instanceof ServerLevel serverLevel) {
                player = serverLevel.getPlayerByUUID(context.playerId);
            }
        }
        
        float playerYaw = 0.0f;
        if (player != null) {
            playerYaw = player.getYRot();
        } else if (!context.beginResults.isEmpty() && context.beginResults.get(0).casterYaw != 0.0f) {
            playerYaw = context.beginResults.get(0).casterYaw;
        }
        
        List<SpellResult> newResults = new ArrayList<>();
        
        for (SpellResult beginResult : context.beginResults) {
            Entity target = beginResult.targetEntity;
            
            if (target != null && target.isAlive()) {
                if (target instanceof BlockGroupEntity blockGroup) {
                    float nearestRotation = blockGroup.getNearest90DegreeRotation(playerYaw);
                    List<BlockPos> placedPositions = blockGroup.placeBlocks(nearestRotation);
                    blockGroup.clearBlocks();
                    
                    if (player != null && !placedPositions.isEmpty()) {
                        for (BlockPos placedPos : placedPositions) {
                            BlockHitResult blockHit = new BlockHitResult(
                                Vec3.atCenterOf(placedPos),
                                Direction.UP,
                                placedPos,
                                false
                            );
                            SpellResult placedBlockResult = SpellResult.fromHitResultWithCaster(
                                blockHit,
                                SpellEffectType.RESOLVED,
                                player
                            );
                            newResults.add(placedBlockResult);
                        }
                    }
                    
                    blockGroup.discard();
                } else {
                    target.noPhysics = false;
                    target.setNoGravity(false);
                    newResults.add(beginResult);
                }
            } else {
                newResults.add(beginResult);
            }
        }
        
        context.beginResults.clear();
        context.beginResults.addAll(newResults);
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;
        if (!(world instanceof ServerLevel serverLevel)) return;
        
        ArsZero.LOGGER.info("[AnchorEffect] onResolveBlock: shooter type: {}, caster type: {}", 
            shooter != null ? shooter.getClass().getSimpleName() : "null",
            spellContext.getCaster() != null ? spellContext.getCaster().getClass().getSimpleName() : "null");
        
        MultiPhaseCastContext castContext = null;
        Vec3 casterPos = null;
        float casterYaw = 0;
        float casterPitch = 0;
        
        if (spellContext.getCaster() instanceof TileCaster tileCaster) {
            BlockEntity tile = tileCaster.getTile();
            ArsZero.LOGGER.info("[AnchorEffect] onResolveBlock: TileCaster detected, tile type: {}", 
                tile != null ? tile.getClass().getSimpleName() : "null");
            if (tile instanceof MultiphaseSpellTurretTile turretTile) {
                castContext = turretTile.getCastContext();
                ArsZero.LOGGER.info("[AnchorEffect] onResolveBlock: Turret tile found, castContext: {}, beginResults: {}", 
                    castContext != null, castContext != null ? castContext.beginResults.size() : 0);
                BlockPos tilePos = turretTile.getBlockPos();
                casterPos = Vec3.atCenterOf(tilePos);
                Direction facing = turretTile.getBlockState().getValue(BasicSpellTurret.FACING);
                casterYaw = directionToYaw(facing);
                casterPitch = directionToPitch(facing);
            }
        } else if (shooter instanceof Player player) {
            ItemStack casterTool = spellContext.getCasterTool();
            castContext = AbstractMultiphaseHandheldDevice.findContextByStack(player, casterTool);
            casterPos = player.getEyePosition(1.0f);
            casterYaw = player.getYRot();
            casterPitch = player.getXRot();
        }
        
        if (castContext == null) {
            ArsZero.LOGGER.warn("[AnchorEffect] onResolveBlock: No context");
            return;
        }
        
        if (castContext.beginResults.isEmpty()) {
            ArsZero.LOGGER.warn("[AnchorEffect] onResolveBlock: No context or empty beginResults");
            return;
        }
        
        ArsZero.LOGGER.info("[AnchorEffect] onResolveBlock: Processing {} beginResults with casterPos: {}, yaw: {}, pitch: {}", 
            castContext.beginResults.size(), casterPos, casterYaw, casterPitch);
        
        for (SpellResult beginResult : castContext.beginResults) {
            ArsZero.LOGGER.info("[AnchorEffect] onResolveBlock: Processing result - blockGroup: {}, relativeOffset: {}, targetEntity: {}, hitResult type: {}", 
                beginResult.blockGroup != null ? beginResult.blockGroup.getClass().getSimpleName() : "null",
                beginResult.relativeOffset != null,
                beginResult.targetEntity != null ? beginResult.targetEntity.getClass().getSimpleName() : "null",
                beginResult.hitResult != null ? beginResult.hitResult.getType().name() : "null");
            
            if (beginResult.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof BlockGroupEntity) {
                ArsZero.LOGGER.info("[AnchorEffect] onResolveBlock: Found BlockGroupEntity in hitResult! Entity: {}", entityHit.getEntity());
            }
            
            BlockGroupEntity blockGroup = null;
            if (beginResult.blockGroup != null) {
                blockGroup = beginResult.blockGroup;
                ArsZero.LOGGER.info("[AnchorEffect] onResolveBlock: Found blockGroup in beginResult.blockGroup");
            } else if (beginResult.targetEntity instanceof BlockGroupEntity bg) {
                blockGroup = bg;
                ArsZero.LOGGER.info("[AnchorEffect] onResolveBlock: Found blockGroup in beginResult.targetEntity");
            } else if (beginResult.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof BlockGroupEntity bg) {
                blockGroup = bg;
                ArsZero.LOGGER.info("[AnchorEffect] onResolveBlock: Found blockGroup in beginResult.hitResult EntityHitResult");
            }
            
            if (blockGroup != null) {
                ArsZero.LOGGER.info("[AnchorEffect] onResolveBlock: Found blockGroup, adding lifespan");
                blockGroup.addLifespan(1);
                
                if (beginResult.relativeOffset != null) {
                    Vec3 newPosition = beginResult.transformLocalToWorld(
                        casterYaw, 
                        casterPitch, 
                        casterPos,
                        castContext.distanceMultiplier
                    );
                    
                    ArsZero.LOGGER.info("[AnchorEffect] onResolveBlock: Calculated newPosition: {}, canMove: {}", 
                        newPosition, newPosition != null && canMoveToPosition(newPosition, world));
                    
                    if (newPosition != null && canMoveToPosition(newPosition, world)) {
                        blockGroup.setPos(newPosition.x, newPosition.y, newPosition.z);
                        blockGroup.setDeltaMovement(Vec3.ZERO);
                        blockGroup.setNoGravity(true);
                        ArsZero.LOGGER.info("[AnchorEffect] onResolveBlock: Moved blockGroup to {}", newPosition);
                    } else {
                        ArsZero.LOGGER.warn("[AnchorEffect] onResolveBlock: Cannot move blockGroup to position: newPosition={}, canMove={}", 
                            newPosition, newPosition != null && canMoveToPosition(newPosition, world));
                    }
                } else {
                    ArsZero.LOGGER.warn("[AnchorEffect] onResolveBlock: blockGroup found but relativeOffset is null, cannot move");
                }
            } else {
                ArsZero.LOGGER.warn("[AnchorEffect] onResolveBlock: No blockGroup found in result");
            }
        }
    }

    @Override
    public int getDefaultManaCost() {
        return 0;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "Increases the distance from the player");
        map.put(AugmentDampen.INSTANCE, "Decreases the distance from the player");
    }

    @Override
    public String getBookDescription() {
        return "When used with Temporal Context Form in TICK phase, keeps the target entity or block group locked to the same position on your screen. The target will follow your look direction and movement. Use with Touch + [Target] in BEGIN, then Temporal Context Form + Anchor in TICK. For blocks, use Select + [Target] in BEGIN. Amplify increases distance from player.";
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<SpellSchool> getSchools() {
        return Set.of(SpellSchools.MANIPULATION);
    }
}

