package com.github.ars_zero.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.CastPhase;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.StaffCastContext;
import com.github.ars_zero.common.spell.WrappedSpellResolver;
import com.github.ars_zero.registry.ModEntities;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.event.SpellResolveEvent;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
    
    // Store block states captured before effects run
    private static final Map<ServerLevel, Map<BlockPos, BlockState>> capturedBlockStates = new ConcurrentHashMap<>();
    
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
                
                // Store in map keyed by level and position
                capturedBlockStates.computeIfAbsent(serverLevel, k -> new HashMap<>()).put(pos, state);
                
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
                                capturedBlockStates.get(serverLevel).put(aoePos, aoeState);
                                
                                // Remove block immediately in PRE event, before effects run
                                event.world.setBlock(aoePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                            }
                        }
                    }
                    
                    // Also remove the main block
                    if (!state.isAir()) {
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
        
        if (wrapped.getPhase() != CastPhase.BEGIN) {
            return;
        }
        
        Player player = ((ServerLevel) event.world).getServer().getPlayerList().getPlayer(wrapped.getPlayerId());
        if (player == null) {
            return;
        }
        
        StaffCastContext context = AbstractSpellStaff.getStaffContext(player);
        if (context == null) {
            return;
        }
        
        HitResult hitResult = event.rayTraceResult;
        SpellResult result = null;
        
        if (hitResult instanceof BlockHitResult blockHit && event.world instanceof ServerLevel serverLevel) {
            BlockPos pos = blockHit.getBlockPos();
            if (!event.world.isOutsideBuildHeight(pos) && BlockUtil.destroyRespectsClaim(player, event.world, pos)) {
                BlockPos targetPos = pos;
                double aoeBuff = event.spellStats.getAoeMultiplier();
                int pierceBuff = event.spellStats.getBuffCount(com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce.INSTANCE);
                List<BlockPos> posList = SpellUtil.calcAOEBlocks(player, targetPos, blockHit, aoeBuff, pierceBuff);
                
                List<BlockPos> validBlocks = new ArrayList<>();
                // Use captured states from PRE event
                Map<BlockPos, BlockState> capturedStates = capturedBlockStates.getOrDefault(serverLevel, new HashMap<>());
                
                for (BlockPos blockPos : posList) {
                    if (!event.world.isOutsideBuildHeight(blockPos) && BlockUtil.destroyRespectsClaim(player, event.world, blockPos)) {
                        // Use captured state if available, otherwise try to read from world
                        var state = capturedStates.get(blockPos);
                        if (state == null) {
                            state = event.world.getBlockState(blockPos);
                        }
                        
                        // Only add if not air
                        if (state != null && !state.isAir()) {
                            validBlocks.add(blockPos);
                            capturedStates.put(blockPos, state); // Ensure it's in the map
                        }
                    }
                }
                
                // Clear captured states for this level after use
                capturedBlockStates.remove(serverLevel);
                
                if (!validBlocks.isEmpty()) {
                    Vec3 centerPos = calculateCenter(validBlocks);
                    
                    BlockGroupEntity blockGroup = new BlockGroupEntity(ModEntities.BLOCK_GROUP.get(), serverLevel);
                    blockGroup.setPos(centerPos.x, centerPos.y, centerPos.z);
                    
                    // Add blocks using captured states - this bypasses reading from world
                    blockGroup.addBlocksWithStates(validBlocks, capturedStates);
                    
                    // Blocks were already removed in PRE event, so we skip removal here
                    
                    serverLevel.addFreshEntity(blockGroup);
                    
                    result = SpellResult.fromBlockGroup(blockGroup, validBlocks, player);
                }
            }
        }
        
        if (result == null) {
            result = SpellResult.fromHitResultWithCaster(hitResult, SpellEffectType.RESOLVED, player);
        }
        
        switch (wrapped.getPhase()) {
            case BEGIN -> {
                context.beginResults.add(result);
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
        
        StaffCastContext context = AbstractSpellStaff.getStaffContext(player);
        if (context == null) {
            return;
        }
        
        context.beginFinished = true;
    }
}
