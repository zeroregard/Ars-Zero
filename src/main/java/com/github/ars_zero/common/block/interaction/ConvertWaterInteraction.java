package com.github.ars_zero.common.block.interaction;

import com.github.ars_zero.common.block.BlightInteraction;
import com.github.ars_zero.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class ConvertWaterInteraction implements BlightInteraction {
    @Override
    public boolean matches(BlockState state) {
        return state.getBlock() == Blocks.WATER || state.getFluidState().is(FluidTags.WATER);
    }
    
    @Override
    public void apply(ServerLevel level, BlockPos pos, BlockState waterState) {
        if (waterState.getBlock() == Blocks.WATER) {
            int waterLevel = waterState.getValue(LiquidBlock.LEVEL);
            BlockState blightState = ModFluids.BLIGHT_FLUID_BLOCK.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, waterLevel);
            level.setBlock(pos, blightState, 3);
        } else {
            FluidState fluidState = waterState.getFluidState();
            if (fluidState.isSource()) {
                level.setBlock(pos, ModFluids.BLIGHT_FLUID_BLOCK.get().defaultBlockState(), 3);
            } else {
                int amount = fluidState.getAmount();
                int levelValue = Math.max(0, Math.min(7, 8 - amount));
                BlockState blightState = ModFluids.BLIGHT_FLUID_BLOCK.get().defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, levelValue);
                level.setBlock(pos, blightState, 3);
            }
        }
    }
    
    public boolean shouldApply(ServerLevel level) {
        return level.getRandom().nextInt(2) == 0;
    }
}
