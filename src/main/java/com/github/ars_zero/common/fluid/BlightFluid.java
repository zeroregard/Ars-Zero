package com.github.ars_zero.common.fluid;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public abstract class BlightFluid extends BaseFlowingFluid {
    
    protected BlightFluid(Properties properties) {
        super(properties);
    }
    
    public static class Source extends BlightFluid {
        public Source(Properties properties) {
            super(properties);
        }
        
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }
        
        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
    
    public static class Flowing extends BlightFluid {
        public Flowing(Properties properties) {
            super(properties);
        }
        
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        
        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }
        
        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
        
        @Override
        protected boolean canConvertToSource(Level level) {
            return false;
        }
    }
}
