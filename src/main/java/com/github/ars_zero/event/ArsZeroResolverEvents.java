package com.github.ars_zero.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.common.glyph.TemporalContextForm;
import com.github.ars_zero.common.spell.IMultiPhaseCaster;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellPhase;
import com.github.ars_zero.common.spell.WrappedSpellResolver;
import com.github.ars_zero.common.particle.timeline.SelectTimeline;
import com.github.ars_zero.common.util.BlockImmutabilityUtil;
import com.github.ars_zero.registry.ModEntities;
import com.github.ars_zero.registry.ModParticleTimelines;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.event.SpellResolveEvent;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.IWrappedCaster;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.TileCaster;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
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
                
                Player player = serverLevel.getServer().getPlayerList().getPlayer(wrapped.getPlayerId());
                if (player == null) {
                    return;
                }
                
                ItemStack casterTool = event.resolver.spellContext.getCasterTool();
                boolean willCreateEntityGroup = requiresEntityGroupForTemporalAnchor(casterTool, player);
                
                if (willCreateEntityGroup && wrapped.isRootResolver()) {
                    // Store in map keyed by dimension and position
                    capturedBlockStates.computeIfAbsent(dimensionKey, k -> new HashMap<>()).put(pos, state);
                    
                    double aoeBuff = event.spellStats.getAoeMultiplier();
                    int pierceBuff = event.spellStats.getBuffCount(com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce.INSTANCE);
                    List<BlockPos> posList = SpellUtil.calcAOEBlocks(player, pos, blockHit, aoeBuff, pierceBuff);
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
        
        IMultiPhaseCaster multiPhaseCaster = IMultiPhaseCaster.from(event.resolver.spellContext, null);
        if (multiPhaseCaster != null) {
            context = multiPhaseCaster.getCastContext();
            if (context != null) {
                if (serverLevel != null) {
                    player = serverLevel.getServer().getPlayerList().getPlayer(wrapped.getPlayerId());
                }
            } else {
                ArsZero.LOGGER.warn("[ArsZeroResolverEvents] IMultiPhaseCaster has no cast context!");
                if (dimensionKey != null) {
                    capturedBlockStates.remove(dimensionKey);
                    blockGroupCreated.remove(dimensionKey);
                }
                return;
            }
        } else {
            player = serverLevel != null ? serverLevel.getServer().getPlayerList().getPlayer(wrapped.getPlayerId()) : null;
            if (player == null) {
                ArsZero.LOGGER.debug("[ArsZeroResolverEvents] No player found for resolver");
                if (dimensionKey != null) {
                    capturedBlockStates.remove(dimensionKey);
                    blockGroupCreated.remove(dimensionKey);
                }
                return;
            }
            
            IMultiPhaseCaster caster = IMultiPhaseCaster.from(event.resolver.spellContext, player);
            if (caster == null) {
                ArsZero.LOGGER.debug("[ArsZeroResolverEvents] No IMultiPhaseCaster found");
                if (dimensionKey != null) {
                    capturedBlockStates.remove(dimensionKey);
                    blockGroupCreated.remove(dimensionKey);
                }
                return;
            }
            context = caster.getCastContext();
            if (context == null) {
                ArsZero.LOGGER.debug("[ArsZeroResolverEvents] No context found for player");
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
        
        if (hitResult instanceof BlockHitResult blockHit && serverLevel != null && dimensionKey != null && player != null) {
            BlockPos pos = blockHit.getBlockPos();
            if (!event.world.isOutsideBuildHeight(pos) && BlockUtil.destroyRespectsClaim(player, event.world, pos)) {
                BlockPos targetPos = pos;
                double aoeBuff = event.spellStats.getAoeMultiplier();
                int pierceBuff = event.spellStats.getBuffCount(com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce.INSTANCE);
                List<BlockPos> posList = SpellUtil.calcAOEBlocks(player, targetPos, blockHit, aoeBuff, pierceBuff);
                
                List<BlockPos> validBlocks = new ArrayList<>();
                // Use captured states from PRE event
                Map<BlockPos, BlockState> capturedStates = capturedBlockStates.getOrDefault(dimensionKey, new HashMap<>());
                
                for (BlockPos blockPos : posList) {
                    if (!event.world.isOutsideBuildHeight(blockPos) && BlockUtil.destroyRespectsClaim(player, event.world, blockPos)) {
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
                
                ItemStack casterTool = event.resolver.spellContext.getCasterTool();
                if (!validBlocks.isEmpty() && !casterTool.isEmpty() && requiresEntityGroupForTemporalAnchor(casterTool, player) && wrapped.isRootResolver()) {
                    if (!blockGroupCreated.getOrDefault(dimensionKey, false)) {
                        Vec3 centerPos = calculateCenter(validBlocks);

                        BlockGroupEntity blockGroup = new BlockGroupEntity(ModEntities.BLOCK_GROUP.get(), serverLevel);
                        blockGroup.setPos(centerPos.x, centerPos.y, centerPos.z);
                        blockGroup.setCasterUUID(player.getUUID());
                        SelectTimeline selectTimeline = event.resolver.spellContext.getParticleTimeline(ModParticleTimelines.SELECT_TIMELINE.get());
                        if (selectTimeline != null) {
                            blockGroup.setOutlineColor(selectTimeline.getColor().getColor());
                        }
                        blockGroup.addBlocksWithStates(validBlocks, capturedStates);
                        
                        serverLevel.addFreshEntity(blockGroup);
                        
                        result = SpellResult.fromBlockGroup(blockGroup, validBlocks, event.resolver.spellContext.getCaster());
                        
                        blockGroupCreated.put(dimensionKey, true);
                    } else {
                        result = SpellResult.fromHitResultWithCaster(hitResult, SpellEffectType.RESOLVED, event.resolver.spellContext.getCaster());
                    }
                    
                    capturedBlockStates.remove(dimensionKey);
                } else {
                    capturedBlockStates.remove(dimensionKey);
                    cleanedUp = true;
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
            result = SpellResult.fromHitResultWithCaster(hitResult, SpellEffectType.RESOLVED, event.resolver.spellContext.getCaster());
        }
        
        switch (wrapped.getPhase()) {
            case BEGIN -> {
                if (result != null && result.hitResult instanceof BlockHitResult && !context.beginResults.isEmpty()) {
                    for (SpellResult existing : context.beginResults) {
                        if (existing != null && existing.hitResult instanceof EntityHitResult) {
                            ArsZero.LOGGER.debug("[ArsZeroResolverEvents] BEGIN: Skipping block result, entity result already exists");
                            return;
                        }
                    }
                }
                if (result != null && result.blockGroup != null) {
                    context.beginResults.clear();
                    context.beginResults.add(result);
                    ArsZero.LOGGER.info("[ArsZeroResolverEvents] BEGIN: Added blockGroup result, beginResults.size={}", 
                        context.beginResults.size());
                } else {
                    context.beginResults.add(result);
                    ArsZero.LOGGER.info("[ArsZeroResolverEvents] BEGIN: Added result, beginResults.size={}, result={}", 
                        context.beginResults.size(), result != null ? result.getClass().getSimpleName() : "null");
                }
            }
            case TICK -> {
                context.tickResults.add(result);
                ArsZero.LOGGER.info("[ArsZeroResolverEvents] TICK: Added result, tickResults.size={}", 
                    context.tickResults.size());
            }
            case END -> {
                context.endResults.add(result);
                ArsZero.LOGGER.info("[ArsZeroResolverEvents] END: Added result, endResults.size={}", 
                    context.endResults.size());
            }
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
        IMultiPhaseCaster caster = null;
        
        if (player != null) {
            caster = IMultiPhaseCaster.from(event.resolver.spellContext, player);
        } else {
            IWrappedCaster wrappedCaster = event.resolver.spellContext.getCaster();
            if (wrappedCaster != null) {
                BlockEntity tile = getBlockEntityFromCaster(wrappedCaster);
                if (tile instanceof IMultiPhaseCaster multiPhaseCaster) {
                    caster = multiPhaseCaster;
                }
            }
        }
        
        if (caster == null) {
            if (dimensionKey != null) {
                capturedBlockStates.remove(dimensionKey);
                blockGroupCreated.remove(dimensionKey);
            }
            return;
        }
        
        MultiPhaseCastContext context = caster.getCastContext();
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
    
    private static BlockEntity getBlockEntityFromCaster(IWrappedCaster wrappedCaster) {
        if (wrappedCaster instanceof TileCaster tileCaster) {
            return tileCaster.getTile();
        }
        return null;
    }
}
