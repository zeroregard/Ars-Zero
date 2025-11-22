package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.config.ServerConfig;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.registry.ModEntities;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
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
        if (!(shooter instanceof Player player)) return;
        if (!(world instanceof ServerLevel serverLevel)) return;
        
        ItemStack casterTool = spellContext.getCasterTool();
        MultiPhaseCastContext castContext = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
        
        if (castContext == null || castContext.beginResults.isEmpty()) {
            return;
        }
        
        for (SpellResult beginResult : castContext.beginResults) {
            Entity target = beginResult.targetEntity;
            
            if (target == null || !target.isAlive()) {
                continue;
            }
            
            if (beginResult.relativeOffset == null) {
                continue;
            }
            
            if (target instanceof Player targetPlayer) {
                if (!canAnchorPlayer(player, targetPlayer, serverLevel)) {
                    continue;
                }
                
                if (!areInSameChunk(player, targetPlayer)) {
                    restoreEntityPhysics(castContext);
                    continue;
                }
            }
            
            Vec3 newPosition = beginResult.transformLocalToWorld(
                player.getYRot(), 
                player.getXRot(), 
                player.getEyePosition(1.0f),
                castContext.distanceMultiplier
            );
            
            if (newPosition != null && canMoveToPosition(newPosition, world)) {
                if (target instanceof ServerPlayer targetPlayer) {
                    targetPlayer.teleportTo(newPosition.x, newPosition.y, newPosition.z);
                    targetPlayer.setDeltaMovement(Vec3.ZERO);
                    targetPlayer.setNoGravity(true);
                } else {
                    target.setPos(newPosition.x, newPosition.y, newPosition.z);
                    target.setDeltaMovement(Vec3.ZERO);
                    target.setNoGravity(true);
                    
                    if (target instanceof BaseVoxelEntity voxel) {
                        voxel.freezePhysics();
                    }
                }
            }
        }
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
        if (!(shooter instanceof Player player)) return;
        
        ItemStack casterTool = spellContext.getCasterTool();
        MultiPhaseCastContext castContext = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
        
        if (castContext == null || castContext.beginResults.isEmpty()) {
            return;
        }
        
        for (SpellResult beginResult : castContext.beginResults) {
            if (beginResult.blockGroup != null && beginResult.relativeOffset != null) {
                BlockGroupEntity blockGroup = beginResult.blockGroup;
                
                Vec3 newPosition = beginResult.transformLocalToWorld(
                    player.getYRot(), 
                    player.getXRot(), 
                    player.getEyePosition(1.0f),
                    castContext.distanceMultiplier
                );
                
                if (newPosition != null && canMoveToPosition(newPosition, world)) {
                    blockGroup.setPos(newPosition.x, newPosition.y, newPosition.z);
                    blockGroup.setDeltaMovement(Vec3.ZERO);
                    blockGroup.setNoGravity(true);
                }
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

