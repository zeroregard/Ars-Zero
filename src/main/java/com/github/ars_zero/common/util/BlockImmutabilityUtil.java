package com.github.ars_zero.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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
}
