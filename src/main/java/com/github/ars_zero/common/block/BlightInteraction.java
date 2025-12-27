package com.github.ars_zero.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface BlightInteraction {
    boolean matches(BlockState state);
    void apply(ServerLevel level, BlockPos pos, BlockState state);
}
