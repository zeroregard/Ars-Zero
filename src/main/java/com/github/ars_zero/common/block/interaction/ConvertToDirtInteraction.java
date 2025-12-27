package com.github.ars_zero.common.block.interaction;

import com.github.ars_zero.common.block.BlightInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ConvertToDirtInteraction implements BlightInteraction {
    @Override
    public boolean matches(BlockState state) {
        return state.getBlock() == Blocks.GRASS_BLOCK ||
               state.getBlock() == Blocks.MYCELIUM ||
               state.getBlock() == Blocks.PODZOL ||
               state.getBlock() == Blocks.COARSE_DIRT ||
               state.getBlock() == Blocks.FARMLAND ||
               state.getBlock() == Blocks.MOSS_BLOCK;
    }
    
    @Override
    public void apply(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
    }
}
