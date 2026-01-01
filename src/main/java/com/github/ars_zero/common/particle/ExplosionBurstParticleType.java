package com.github.ars_zero.common.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ExplosionBurstParticleType extends ParticleType<ExplosionBurstParticleOptions> {
    public ExplosionBurstParticleType() {
        super(false);
    }
    
    @Override
    public MapCodec<ExplosionBurstParticleOptions> codec() {
        return ExplosionBurstParticleOptions.CODEC;
    }
    
    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ExplosionBurstParticleOptions> streamCodec() {
        return ExplosionBurstParticleOptions.STREAM_CODEC;
    }
}

