package com.github.ars_zero.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

public class BlockImmutabilityUtil {
    
    public static boolean isBlockImmutable(Block block) {
        return block == Blocks.BEDROCK 
            || block == Blocks.BARRIER 
            || block == Blocks.COMMAND_BLOCK 
            || block == Blocks.CHAIN_COMMAND_BLOCK 
            || block == Blocks.REPEATING_COMMAND_BLOCK;
    }
    
    public static boolean isBlockImmutable(BlockState state) {
        return isBlockImmutable(state.getBlock());
    }
    
    public static boolean isBlockImmutable(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return isBlockImmutable(state);
    }
    
    public static boolean canBlockBeDestroyed(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }
        
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        
        if (isBlockImmutable(block)) {
            return false;
        }
        
        float destroySpeed = state.getDestroySpeed(level, pos);
        if (destroySpeed < 0.0f) {
            return false;
        }
        
        return true;
    }

    public static boolean canBlockBeGrouped(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }
        return canBlockBeGrouped(level, pos, level.getBlockState(pos));
    }

    public static boolean canBlockBeGrouped(Level level, BlockPos pos, BlockState state) {
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }
        if (state == null || state.isAir()) {
            return false;
        }
        if (isBlockImmutable(state)) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0.0f) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.hasBlockEntity() || level.getBlockEntity(pos) != null) {
            return false;
        }
        return !hasBlockCapabilities(level, pos);
    }

    private static boolean hasBlockCapabilities(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction) != null) {
                return true;
            }
            if (level.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction) != null) {
                return true;
            }
            if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction) != null) {
                return true;
            }
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null
            || level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null) != null
            || level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) != null;
    }
}
