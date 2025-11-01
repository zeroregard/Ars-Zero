package com.github.ars_zero.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.common.item.ArsZeroStaff;
import com.github.ars_zero.common.spell.CastPhase;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.StaffCastContext;
import com.github.ars_zero.common.spell.WrappedSpellResolver;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.event.SpellResolveEvent;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "ars_zero")
public class ArsZeroResolverEvents {
    
    // Store block states captured before effects run
    private static final java.util.Map<net.minecraft.server.level.ServerLevel, java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState>> capturedBlockStates = new java.util.concurrent.ConcurrentHashMap<>();
    
    @SubscribeEvent
    public static void onEffectResolving(com.hollingsworth.arsnouveau.api.event.EffectResolveEvent.Pre event) {
        if (event.world.isClientSide) {
            return;
        }
        
        if (!(event.resolver instanceof WrappedSpellResolver wrapped)) {
            return;
        }
        
        if (wrapped.getPhase() != CastPhase.BEGIN) {
            return;
        }
        
        // Capture block states BEFORE any effects resolve
        if (event.rayTraceResult instanceof BlockHitResult blockHit && event.world instanceof ServerLevel serverLevel) {
            BlockPos pos = blockHit.getBlockPos();
            if (!event.world.isOutsideBuildHeight(pos)) {
                var state = event.world.getBlockState(pos);
                ArsZero.LOGGER.info("[ResolverEvents] PRE: Capturing block at {}: state={}, isAir={}", 
                    pos, state.getBlock().getDescriptionId(), state.isAir());
                
                // Store in map keyed by level and position
                capturedBlockStates.computeIfAbsent(serverLevel, k -> new java.util.HashMap<>()).put(pos, state);
                
                // Also capture AOE blocks and remove them immediately
                double aoeBuff = event.spellStats.getAoeMultiplier();
                int pierceBuff = event.spellStats.getBuffCount(com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce.INSTANCE);
                Player player = serverLevel.getServer().getPlayerList().getPlayer(wrapped.getPlayerId());
                if (player != null) {
                    List<BlockPos> posList = SpellUtil.calcAOEBlocks(player, pos, blockHit, aoeBuff, pierceBuff);
                    for (BlockPos aoePos : posList) {
                        if (!event.world.isOutsideBuildHeight(aoePos)) {
                            var aoeState = event.world.getBlockState(aoePos);
                            if (!aoeState.isAir()) {
                                ArsZero.LOGGER.info("[ResolverEvents] PRE: Capturing AOE block at {}: state={}", 
                                    aoePos, aoeState.getBlock().getDescriptionId());
                                capturedBlockStates.get(serverLevel).put(aoePos, aoeState);
                                
                                // Remove block immediately in PRE event, before effects run
                                boolean removed = event.world.setBlock(aoePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                                ArsZero.LOGGER.info("[ResolverEvents] PRE: Removed block at {}: result={}", aoePos, removed);
                            }
                        }
                    }
                    
                    // Also remove the main block
                    if (!state.isAir()) {
                        boolean removed = event.world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                        ArsZero.LOGGER.info("[ResolverEvents] PRE: Removed main block at {}: result={}", pos, removed);
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
        
        if (wrapped.getPhase() != CastPhase.BEGIN) {
            return;
        }
        
        Player player = ((ServerLevel) event.world).getServer().getPlayerList().getPlayer(wrapped.getPlayerId());
        if (player == null) {
            return;
        }
        
        StaffCastContext context = ArsZeroStaff.getStaffContext(player);
        if (context == null) {
            ArsZero.LOGGER.debug("[ResolverEvents] StaffCastContext is null for player {}", player.getName().getString());
            return;
        }
        
        HitResult hitResult = event.rayTraceResult;
        ArsZero.LOGGER.info("[ResolverEvents] Effect resolved in BEGIN phase. HitResult type: {}", hitResult.getType());
        
        SpellResult result = null;
        
        if (hitResult instanceof BlockHitResult blockHit && event.world instanceof ServerLevel serverLevel) {
            ArsZero.LOGGER.info("[ResolverEvents] Block hit at {}", blockHit.getBlockPos());
            
            BlockPos pos = blockHit.getBlockPos();
            if (!event.world.isOutsideBuildHeight(pos) && BlockUtil.destroyRespectsClaim(player, event.world, pos)) {
                BlockPos targetPos = pos;
                double aoeBuff = event.spellStats.getAoeMultiplier();
                int pierceBuff = event.spellStats.getBuffCount(com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce.INSTANCE);
                List<BlockPos> posList = SpellUtil.calcAOEBlocks(player, targetPos, blockHit, aoeBuff, pierceBuff);
                
                ArsZero.LOGGER.info("[ResolverEvents] Calculated {} AOE blocks", posList.size());
                
                List<BlockPos> validBlocks = new ArrayList<>();
                // Use captured states from PRE event
                java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> capturedStates = capturedBlockStates.getOrDefault(serverLevel, new java.util.HashMap<>());
                ArsZero.LOGGER.info("[ResolverEvents] POST: Retrieved {} captured states from PRE event", capturedStates.size());
                
                for (BlockPos blockPos : posList) {
                    if (!event.world.isOutsideBuildHeight(blockPos) && BlockUtil.destroyRespectsClaim(player, event.world, blockPos)) {
                        // Use captured state if available, otherwise try to read from world
                        var state = capturedStates.get(blockPos);
                        if (state == null) {
                            state = event.world.getBlockState(blockPos);
                            ArsZero.LOGGER.warn("[ResolverEvents] POST: No captured state for {}, reading from world: state={}, isAir={}", 
                                blockPos, state.getBlock().getDescriptionId(), state.isAir());
                        } else {
                            ArsZero.LOGGER.info("[ResolverEvents] POST: Using captured state for {}: state={}", 
                                blockPos, state.getBlock().getDescriptionId());
                        }
                        
                        // Only add if not air
                        if (state != null && !state.isAir()) {
                            validBlocks.add(blockPos);
                            capturedStates.put(blockPos, state); // Ensure it's in the map
                        } else {
                            ArsZero.LOGGER.warn("[ResolverEvents] POST: Block at {} is air (state={}), skipping", blockPos, state);
                        }
                    }
                }
                
                // Clear captured states for this level after use
                capturedBlockStates.remove(serverLevel);
                
                if (!validBlocks.isEmpty()) {
                    ArsZero.LOGGER.info("[ResolverEvents] Creating BlockGroupEntity with {} valid blocks", validBlocks.size());
                    
                    Vec3 centerPos = calculateCenter(validBlocks);
                    ArsZero.LOGGER.info("[ResolverEvents] Center position calculated: {}", centerPos);
                    
                    BlockGroupEntity blockGroup = new BlockGroupEntity(ModEntities.BLOCK_GROUP.get(), serverLevel);
                    blockGroup.setPos(centerPos.x, centerPos.y, centerPos.z);
                    ArsZero.LOGGER.info("[ResolverEvents] BlockGroupEntity created at position: {}", blockGroup.position());
                    
                    // Add blocks using captured states - this bypasses reading from world
                    blockGroup.addBlocksWithStates(validBlocks, capturedStates);
                    ArsZero.LOGGER.info("[ResolverEvents] BlockGroupEntity added {} blocks", blockGroup.getBlockCount());
                    
                    if (blockGroup.isEmpty()) {
                        ArsZero.LOGGER.error("[ResolverEvents] ERROR: BlockGroupEntity is empty after addBlocks! Valid blocks were: {}", validBlocks);
                        ArsZero.LOGGER.error("[ResolverEvents] This means blocks were not added properly. Check block states.");
                    } else {
                        // Blocks were already removed in PRE event, so we skip removal here
                        ArsZero.LOGGER.info("[ResolverEvents] Blocks were already removed in PRE event, skipping removal");
                    }
                    
                    serverLevel.addFreshEntity(blockGroup);
                    ArsZero.LOGGER.info("[ResolverEvents] BlockGroupEntity spawned with ID {} at position {}", blockGroup.getId(), blockGroup.position());
                    
                    result = SpellResult.fromBlockGroup(blockGroup, validBlocks, player);
                    ArsZero.LOGGER.info("[ResolverEvents] Created SpellResult with BlockGroupEntity. BlockGroup: {}, BlockPositions: {}", 
                        result.blockGroup != null ? result.blockGroup.getId() : "null",
                        result.blockPositions != null ? result.blockPositions.size() : "null");
                }
            } else {
                ArsZero.LOGGER.debug("[ResolverEvents] Block validation failed - outside build height or claim check failed");
            }
        }
        
        if (result == null) {
            result = SpellResult.fromHitResultWithCaster(hitResult, SpellEffectType.RESOLVED, player);
            ArsZero.LOGGER.debug("[ResolverEvents] Created SpellResult from HitResult (entity or invalid block)");
        }
        
        switch (wrapped.getPhase()) {
            case BEGIN -> {
                context.beginResults.add(result);
                ArsZero.LOGGER.info("[ResolverEvents] Added SpellResult to beginResults. Total results: {}", context.beginResults.size());
            }
            case TICK -> {
                context.tickResults.add(result);
            }
            case END -> {
                context.endResults.add(result);
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
    
    @SubscribeEvent
    public static void onSpellResolved(SpellResolveEvent.Post event) {
        if (event.world.isClientSide) {
            return;
        }
        
        if (!(event.resolver instanceof WrappedSpellResolver wrapped)) {
            return;
        }
        
        if (!wrapped.isRootResolver() || wrapped.getPhase() != CastPhase.BEGIN) {
            return;
        }
        
        Player player = ((ServerLevel) event.world).getServer().getPlayerList().getPlayer(wrapped.getPlayerId());
        if (player == null) {
            return;
        }
        
        StaffCastContext context = ArsZeroStaff.getStaffContext(player);
        if (context == null) {
            return;
        }
        
        context.beginFinished = true;
    }
}
