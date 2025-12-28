package com.github.ars_zero.common.fluid;

import com.github.ars_zero.ArsZero;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Consumer;

public class BlightFluidType extends FluidType {
    
    public BlightFluidType(Properties properties) {
        super(properties);
    }
    
    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL_TEXTURE = ArsZero.prefix("block/blight_fluid_still");
            private static final ResourceLocation FLOWING_TEXTURE = ArsZero.prefix("block/blight_fluid_flowing");
            
            @Override
            public ResourceLocation getStillTexture() {
                return STILL_TEXTURE;
            }
            
            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING_TEXTURE;
            }
        });
    }
}
