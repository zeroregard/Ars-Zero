package com.github.ars_zero.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.block.MultiphaseSpellTurretTile;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.common.glyph.TemporalContextForm;
import com.github.ars_zero.common.item.AbstractMultiphaseHandheldDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellPhase;
import com.github.ars_zero.common.spell.WrappedSpellResolver;
import com.hollingsworth.arsnouveau.common.block.BasicSpellTurret;
import com.github.ars_zero.common.util.BlockImmutabilityUtil;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.TileCaster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.event.SpellResolveEvent;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "ars_zero")
public class ArsZeroResolverEvents {
    
    private static final Map<ResourceKey<Level>, Map<BlockPos, BlockState>> capturedBlockStates = new ConcurrentHashMap<>();
    
    private static final Map<ResourceKey<Level>, Boolean> blockGroupCreated = new ConcurrentHashMap<>();
    
    @SubscribeEvent
    public static void onEffectResolving(com.hollingsworth.arsnouveau.api.event.EffectResolveEvent.Pre event) {
        if (event.world.isClientSide) {
            return;
        }
        
        if (!(event.resolver instanceof WrappedSpellResolver wrapped)) {
            return;
        }
        
        if (wrapped.getPhase() != SpellPhase.BEGIN) {
            return;
        }
        
        // Capture block states BEFORE any effects resolve
        if (event.rayTraceResult instanceof BlockHitResult blockHit && event.world instanceof ServerLevel serverLevel) {
            ResourceKey<Level> dimensionKey = serverLevel.dimension();
            BlockPos pos = blockHit.getBlockPos();
            if (!event.world.isOutsideBuildHeight(pos)) {
                var state = event.world.getBlockState(pos);
                
                boolean willCreateEntityGroup = false;
                Player player = serverLevel.getServer().getPlayerList().getPlayer(wrapped.getPlayerId());
                
                if (event.resolver.spellContext.getCaster() instanceof TileCaster tileCaster) {
                    if (tileCaster.getTile() instanceof MultiphaseSpellTurretTile turretTile) {
                        willCreateEntityGroup = requiresEntityGroupForTemporalAnchorTurret(turretTile);
                    }
                } else if (player != null) {
                    ItemStack casterTool = event.resolver.spellContext.getCasterTool();
                    willCreateEntityGroup = requiresEntityGroupForTemporalAnchor(casterTool, player);
                }
                
                if (willCreateEntityGroup && wrapped.isRootResolver()) {
                    // Store in map keyed by dimension and position
                    capturedBlockStates.computeIfAbsent(dimensionKey, k -> new HashMap<>()).put(pos, state);
                    
                    double aoeBuff = event.spellStats.getAoeMultiplier();
                    int pierceBuff = event.spellStats.getBuffCount(com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce.INSTANCE);
                    List<BlockPos> posList;
                    if (player != null) {
                        posList = SpellUtil.calcAOEBlocks(player, pos, blockHit, aoeBuff, pierceBuff);
                    } else {
                        posList = java.util.Collections.singletonList(pos);
                    }
                    for (BlockPos aoePos : posList) {
                        if (!event.world.isOutsideBuildHeight(aoePos)) {
                            var aoeState = event.world.getBlockState(aoePos);
                            if (!aoeState.isAir() && !BlockImmutabilityUtil.isBlockImmutable(aoeState)) {
                                capturedBlockStates.get(dimensionKey).put(aoePos, aoeState);
                                event.world.setBlock(aoePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                            }
                        }
                    }
                    
                    if (!state.isAir() && !BlockImmutabilityUtil.isBlockImmutable(state)) {
                        event.world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onEffectResolved(EffectResolveEvent.Post event) {
        if (event.world.isClientSide) {
            return;
        }
        
        if (!(event.resolver instanceof WrappedSpellResolver wrapped)) {
            return;
        }
        
        ArsZero.LOGGER.info("[ArsZeroResolverEvents] onEffectResolved: phase: {}, hitResult type: {}", 
            wrapped.getPhase(), event.rayTraceResult != null ? event.rayTraceResult.getType().name() : "null");
        
        if (wrapped.getPhase() != SpellPhase.BEGIN) {
            return;
        }
        
        ServerLevel serverLevel = null;
        ResourceKey<Level> dimensionKey = null;
        if (event.world instanceof ServerLevel level) {
            serverLevel = level;
            dimensionKey = level.dimension();
        }
        
        MultiPhaseCastContext context = null;
        Player player = null;
        
        if (event.resolver.spellContext.getCaster() instanceof TileCaster tileCaster) {
            if (tileCaster.getTile() instanceof MultiphaseSpellTurretTile turretTile) {
                context = turretTile.getCastContext();
                if (context == null) {
                    if (dimensionKey != null) {
                        capturedBlockStates.remove(dimensionKey);
                        blockGroupCreated.remove(dimensionKey);
                    }
                    return;
                }
                if (serverLevel != null) {
                    player = serverLevel.getServer().getPlayerList().getPlayer(wrapped.getPlayerId());
                }
            } else {
                if (dimensionKey != null) {
                    capturedBlockStates.remove(dimensionKey);
                    blockGroupCreated.remove(dimensionKey);
                }
                return;
            }
        } else {
            player = serverLevel != null ? serverLevel.getServer().getPlayerList().getPlayer(wrapped.getPlayerId()) : null;
            if (player == null) {
                if (dimensionKey != null) {
                    capturedBlockStates.remove(dimensionKey);
                    blockGroupCreated.remove(dimensionKey);
                }
                return;
            }
            
            ItemStack casterTool = event.resolver.spellContext.getCasterTool();
            context = AbstractMultiphaseHandheldDevice.findContextByStack(player, casterTool);
            if (context == null) {
                if (dimensionKey != null) {
                    capturedBlockStates.remove(dimensionKey);
                    blockGroupCreated.remove(dimensionKey);
                }
                return;
            }
        }
        
        HitResult hitResult = event.rayTraceResult;
        SpellResult result = null;
        boolean cleanedUp = false;
        
        boolean isTurret = event.resolver.spellContext.getCaster() instanceof TileCaster;
        
        if (hitResult instanceof BlockHitResult blockHit && serverLevel != null && dimensionKey != null) {
            BlockPos pos = blockHit.getBlockPos();
            boolean canDestroy = isTurret || (player != null && BlockUtil.destroyRespectsClaim(player, event.world, pos));
            
            if (!event.world.isOutsideBuildHeight(pos) && canDestroy) {
                BlockPos targetPos = pos;
                double aoeBuff = event.spellStats.getAoeMultiplier();
                int pierceBuff = event.spellStats.getBuffCount(com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce.INSTANCE);
                List<BlockPos> posList;
                if (player != null) {
                    posList = SpellUtil.calcAOEBlocks(player, targetPos, blockHit, aoeBuff, pierceBuff);
                } else if (isTurret) {
                    posList = java.util.Collections.singletonList(targetPos);
                } else {
                    posList = java.util.Collections.singletonList(targetPos);
                }
                
                List<BlockPos> validBlocks = new ArrayList<>();
                // Use captured states from PRE event
                Map<BlockPos, BlockState> capturedStates = capturedBlockStates.getOrDefault(dimensionKey, new HashMap<>());
                
                for (BlockPos blockPos : posList) {
                    boolean canDestroyPos = isTurret || (player != null && BlockUtil.destroyRespectsClaim(player, event.world, blockPos));
                    if (!event.world.isOutsideBuildHeight(blockPos) && canDestroyPos) {
                        var state = capturedStates.get(blockPos);
                        if (state == null) {
                            state = event.world.getBlockState(blockPos);
                        }
                        
                        if (state != null && !state.isAir() && !BlockImmutabilityUtil.isBlockImmutable(state)) {
                            validBlocks.add(blockPos);
                            capturedStates.put(blockPos, state);
                        }
                    }
                }
                
                capturedBlockStates.remove(dimensionKey);
                cleanedUp = true;
                
                boolean shouldCreateBlockGroup = false;
                
                if (event.resolver.spellContext.getCaster() instanceof TileCaster tileCaster) {
                    if (tileCaster.getTile() instanceof MultiphaseSpellTurretTile turretTile) {
                        shouldCreateBlockGroup = requiresEntityGroupForTemporalAnchorTurret(turretTile);
                        ArsZero.LOGGER.info("[ArsZeroResolverEvents] Turret check - shouldCreateBlockGroup: {}, validBlocks: {}", 
                            shouldCreateBlockGroup, validBlocks.size());
                    }
                } else {
                    ItemStack casterTool = event.resolver.spellContext.getCasterTool();
                    if (!casterTool.isEmpty() && player != null) {
                        shouldCreateBlockGroup = requiresEntityGroupForTemporalAnchor(casterTool, player);
                    }
                }
                
                ArsZero.LOGGER.info("[ArsZeroResolverEvents] BlockGroup creation check - shouldCreate: {}, validBlocks: {}, player: {}, isTurret: {}", 
                    shouldCreateBlockGroup, validBlocks.size(), player != null, isTurret);
                
                if (!validBlocks.isEmpty() && shouldCreateBlockGroup && wrapped.isRootResolver()) {
                    if (!blockGroupCreated.getOrDefault(dimensionKey, false)) {
                        Vec3 centerPos = calculateCenter(validBlocks);
                        
                        BlockGroupEntity blockGroup = new BlockGroupEntity(ModEntities.BLOCK_GROUP.get(), serverLevel);
                        blockGroup.setPos(centerPos.x, centerPos.y, centerPos.z);
                        if (player != null) {
                            blockGroup.setCasterUUID(player.getUUID());
                        } else if (isTurret && context != null && context.playerId != null) {
                            blockGroup.setCasterUUID(context.playerId);
                        }
                        
                        blockGroup.addBlocksWithStates(validBlocks, capturedStates);
                        
                        serverLevel.addFreshEntity(blockGroup);
                        
                        if (isTurret && event.resolver.spellContext.getCaster() instanceof TileCaster tileCaster) {
                            if (tileCaster.getTile() instanceof MultiphaseSpellTurretTile turretTile) {
                                BlockPos tilePos = turretTile.getBlockPos();
                                Vec3 turretCasterPos = Vec3.atCenterOf(tilePos);
                                Direction facing = turretTile.getBlockState().getValue(BasicSpellTurret.FACING);
                                float turretYaw = directionToYaw(facing);
                                float turretPitch = directionToPitch(facing);
                                
                                Vec3 relativeOffset = SpellResult.calculateRelativeOffsetInLocalSpace(
                                    turretCasterPos, centerPos, turretYaw, turretPitch
                                );
                                
                                EntityHitResult fakeHit = new EntityHitResult(blockGroup);
                                result = new SpellResult(blockGroup, null, fakeHit, SpellEffectType.RESOLVED,
                                    relativeOffset, turretYaw, turretPitch, turretCasterPos,
                                    blockGroup, validBlocks);
                                
                                ArsZero.LOGGER.info("[ArsZeroResolverEvents] Created BlockGroupEntity for turret with {} blocks, using turret position/rotation", validBlocks.size());
                            } else {
                                result = SpellResult.fromBlockGroup(blockGroup, validBlocks, player);
                            }
                        } else {
                            result = SpellResult.fromBlockGroup(blockGroup, validBlocks, player);
                            ArsZero.LOGGER.info("[ArsZeroResolverEvents] Created BlockGroupEntity with {} blocks", validBlocks.size());
                        }
                        
                        blockGroupCreated.put(dimensionKey, true);
                    } else {
                        result = SpellResult.fromHitResultWithCaster(hitResult, SpellEffectType.RESOLVED, player);
                    }
                }
            } else {
                if (dimensionKey != null) {
                    capturedBlockStates.remove(dimensionKey);
                    blockGroupCreated.remove(dimensionKey);
                    cleanedUp = true;
                }
            }
        }
        
        if (!cleanedUp && dimensionKey != null) {
            capturedBlockStates.remove(dimensionKey);
            blockGroupCreated.remove(dimensionKey);
        }
        
        if (result == null) {
            result = SpellResult.fromHitResultWithCaster(hitResult, SpellEffectType.RESOLVED, player);
        }
        
        switch (wrapped.getPhase()) {
            case BEGIN -> {
                if (result != null && result.blockGroup != null) {
                    context.beginResults.clear();
                    context.beginResults.add(result);
                } else {
                    context.beginResults.add(result);
                }
                ArsZero.LOGGER.info("[ArsZeroResolverEvents] Added result to beginResults - blockGroup: {}, targetEntity: {}, hitResult type: {}", 
                    result.blockGroup != null, result.targetEntity != null, result.hitResult != null ? result.hitResult.getType().name() : "null");
            }
            case TICK -> {
                context.tickResults.add(result);
                ArsZero.LOGGER.info("[ArsZeroResolverEvents] Added result to tickResults - blockGroup: {}, targetEntity: {}, hitResult type: {}", 
                    result.blockGroup != null, result.targetEntity != null, result.hitResult != null ? result.hitResult.getType().name() : "null");
            }
            case END -> context.endResults.add(result);
        }
    }
    
    private static Vec3 calculateCenter(List<BlockPos> positions) {
        if (positions.isEmpty()) return Vec3.ZERO;
        
        double x = 0, y = 0, z = 0;
        for (BlockPos pos : positions) {
            x += pos.getX() + 0.5;
            y += pos.getY() + 0.5;
            z += pos.getZ() + 0.5;
        }
        
        int count = positions.size();
        return new Vec3(x / count, y / count, z / count);
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
    
    private static boolean requiresEntityGroupForTemporalAnchor(ItemStack casterTool, Player player) {
        if (casterTool.isEmpty()) {
            return false;
        }
        
        AbstractCaster<?> caster = SpellCasterRegistry.from(casterTool);
        if (caster == null) {
            return false;
        }
        
        int currentLogicalSlot = caster.getCurrentSlot();
        if (currentLogicalSlot < 0 || currentLogicalSlot >= 10) {
            return false;
        }
        
        boolean hasTemporalContextFormInTick = false;
        boolean hasTemporalContextFormInEnd = false;
        boolean hasAnchorInTick = false;
        boolean hasAnchorInEnd = false;
        
        int tickPhysicalSlot = currentLogicalSlot * 3 + 1;
        int endPhysicalSlot = currentLogicalSlot * 3 + 2;
        
        Spell tickSpell = caster.getSpell(tickPhysicalSlot);
        Spell endSpell = caster.getSpell(endPhysicalSlot);
        
        if (!tickSpell.isEmpty()) {
            for (AbstractSpellPart part : tickSpell.recipe()) {
                if (part instanceof TemporalContextForm) {
                    hasTemporalContextFormInTick = true;
                }
                if (part instanceof AnchorEffect) {
                    hasAnchorInTick = true;
                }
            }
        }
        
        if (!endSpell.isEmpty()) {
            for (AbstractSpellPart part : endSpell.recipe()) {
                if (part instanceof TemporalContextForm) {
                    hasTemporalContextFormInEnd = true;
                }
                if (part instanceof AnchorEffect) {
                    hasAnchorInEnd = true;
                }
            }
        }
        
        boolean hasTemporalContextForm = hasTemporalContextFormInTick || hasTemporalContextFormInEnd;
        boolean hasAnchor = hasAnchorInTick || hasAnchorInEnd;
        
        return hasTemporalContextForm && hasAnchor;
    }
    
    private static boolean requiresEntityGroupForTemporalAnchorTurret(MultiphaseSpellTurretTile turretTile) {
        boolean hasTemporalContextFormInTick = false;
        boolean hasTemporalContextFormInEnd = false;
        boolean hasAnchorInTick = false;
        boolean hasAnchorInEnd = false;
        
        Spell tickSpell = turretTile.getTickSpell();
        Spell endSpell = turretTile.getEndSpell();
        
        ArsZero.LOGGER.info("[ArsZeroResolverEvents] Checking turret spells - tickSpell empty: {}, endSpell empty: {}", 
            tickSpell.isEmpty(), endSpell.isEmpty());
        
        if (!tickSpell.isEmpty()) {
            for (AbstractSpellPart part : tickSpell.recipe()) {
                if (part instanceof TemporalContextForm) {
                    hasTemporalContextFormInTick = true;
                    ArsZero.LOGGER.info("[ArsZeroResolverEvents] Found TemporalContextForm in tick spell");
                }
                if (part instanceof AnchorEffect) {
                    hasAnchorInTick = true;
                    ArsZero.LOGGER.info("[ArsZeroResolverEvents] Found AnchorEffect in tick spell");
                }
            }
        }
        
        if (!endSpell.isEmpty()) {
            for (AbstractSpellPart part : endSpell.recipe()) {
                if (part instanceof TemporalContextForm) {
                    hasTemporalContextFormInEnd = true;
                    ArsZero.LOGGER.info("[ArsZeroResolverEvents] Found TemporalContextForm in end spell");
                }
                if (part instanceof AnchorEffect) {
                    hasAnchorInEnd = true;
                    ArsZero.LOGGER.info("[ArsZeroResolverEvents] Found AnchorEffect in end spell");
                }
            }
        }
        
        boolean hasTemporalContextForm = hasTemporalContextFormInTick || hasTemporalContextFormInEnd;
        boolean hasAnchor = hasAnchorInTick || hasAnchorInEnd;
        
        ArsZero.LOGGER.info("[ArsZeroResolverEvents] Turret check result - hasTemporalContextForm: {}, hasAnchor: {}, result: {}", 
            hasTemporalContextForm, hasAnchor, hasTemporalContextForm && hasAnchor);
        
        return hasTemporalContextForm && hasAnchor;
    }
    
    @SubscribeEvent
    public static void onSpellResolved(SpellResolveEvent.Post event) {
        if (event.world.isClientSide) {
            return;
        }
        
        if (!(event.resolver instanceof WrappedSpellResolver wrapped)) {
            return;
        }
        
        if (!wrapped.isRootResolver() || wrapped.getPhase() != SpellPhase.BEGIN) {
            return;
        }
        
        ServerLevel serverLevel = null;
        ResourceKey<Level> dimensionKey = null;
        if (event.world instanceof ServerLevel level) {
            serverLevel = level;
            dimensionKey = level.dimension();
        }
        
        Player player = serverLevel != null ? serverLevel.getServer().getPlayerList().getPlayer(wrapped.getPlayerId()) : null;
        if (player == null) {
            if (dimensionKey != null) {
                capturedBlockStates.remove(dimensionKey);
                blockGroupCreated.remove(dimensionKey);
            }
            return;
        }
        
        ItemStack casterTool = event.resolver.spellContext.getCasterTool();
        MultiPhaseCastContext context = AbstractMultiphaseHandheldDevice.findContextByStack(player, casterTool);
        if (context == null) {
            if (dimensionKey != null) {
                capturedBlockStates.remove(dimensionKey);
                blockGroupCreated.remove(dimensionKey);
            }
            return;
        }
        
        if (dimensionKey != null) {
            capturedBlockStates.remove(dimensionKey);
            blockGroupCreated.remove(dimensionKey);
        }
        
        context.beginFinished = true;
    }
}
