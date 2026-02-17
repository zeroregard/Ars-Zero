package com.github.ars_zero.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

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

    /**
     * True if a piston can push this block (NORMAL or PUSH_ONLY) and it has no block entity.
     * Used to restrict block group entity creation to blocks that are movable by pistons.
     * Blocks with block entities (chests, furnaces, etc.) are excluded—pistons destroy them.
     */
    public static boolean isPistonPushable(BlockState state) {
        if (state.hasBlockEntity()) {
            return false;
        }
        PushReaction reaction = state.getPistonPushReaction();
        return reaction == PushReaction.NORMAL || reaction == PushReaction.PUSH_ONLY;
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
