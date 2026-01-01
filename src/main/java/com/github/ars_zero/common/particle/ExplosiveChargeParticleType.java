package com.github.ars_zero.common.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ExplosiveChargeParticleType extends ParticleType<ExplosiveChargeParticleOptions> {
    public ExplosiveChargeParticleType() {
        super(false);
    }
    
    @Override
    public MapCodec<ExplosiveChargeParticleOptions> codec() {
        return ExplosiveChargeParticleOptions.CODEC;
    }
    
    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ExplosiveChargeParticleOptions> streamCodec() {
        return ExplosiveChargeParticleOptions.STREAM_CODEC;
    }
}

