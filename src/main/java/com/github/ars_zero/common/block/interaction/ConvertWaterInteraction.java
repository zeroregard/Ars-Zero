package com.github.ars_zero.common.block.interaction;

import com.github.ars_zero.common.block.BlightInteraction;
import com.github.ars_zero.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ConvertWaterInteraction implements BlightInteraction {
    @Override
    public boolean matches(BlockState state) {
        return state.getBlock() == ModFluids.BLIGHT_FLUID_BLOCK.get();
    }
    
    @Override
    public void apply(ServerLevel level, BlockPos pos, BlockState blightState) {
        if (blightState.getBlock() == ModFluids.BLIGHT_FLUID_BLOCK.get()) {
            int blightLevel = blightState.getValue(LiquidBlock.LEVEL);
            BlockState waterState = Blocks.WATER.defaultBlockState()
                .setValue(LiquidBlock.LEVEL, blightLevel);
            level.setBlock(pos, waterState, 3);
        }
    }
    
    public boolean shouldApply(ServerLevel level) {
        return true;
    }
}
