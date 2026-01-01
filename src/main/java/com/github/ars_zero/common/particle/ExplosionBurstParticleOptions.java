package com.github.ars_zero.common.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ExplosionBurstParticleOptions implements ParticleOptions {
    
    protected ParticleType<? extends ExplosionBurstParticleOptions> type;
    
    public static final MapCodec<ExplosionBurstParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(d -> d.r),
            Codec.FLOAT.fieldOf("g").forGetter(d -> d.g),
            Codec.FLOAT.fieldOf("b").forGetter(d -> d.b)
        )
        .apply(instance, (r, g, b) -> new ExplosionBurstParticleOptions(null, r, g, b)));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ExplosionBurstParticleOptions> STREAM_CODEC = StreamCodec.of(
            ExplosionBurstParticleOptions::toNetwork, ExplosionBurstParticleOptions::fromNetwork
    );
    
    public static void toNetwork(RegistryFriendlyByteBuf buf, ExplosionBurstParticleOptions data) {
        buf.writeFloat(data.r);
        buf.writeFloat(data.g);
        buf.writeFloat(data.b);
    }
    
    public static ExplosionBurstParticleOptions fromNetwork(RegistryFriendlyByteBuf buffer) {
        float r = buffer.readFloat();
        float g = buffer.readFloat();
        float b = buffer.readFloat();
        return new ExplosionBurstParticleOptions(null, r, g, b);
    }
    
    public float r;
    public float g;
    public float b;
    
    public ExplosionBurstParticleOptions(float r, float g, float b) {
        this(null, r, g, b);
    }
    
    public ExplosionBurstParticleOptions(ParticleType<? extends ExplosionBurstParticleOptions> type, float r, float g, float b) {
        this.type = type;
        this.r = r;
        this.g = g;
        this.b = b;
    }
    
    public void setType(ParticleType<? extends ExplosionBurstParticleOptions> type) {
        this.type = type;
    }
    
    @Override
    public ParticleType<? extends ExplosionBurstParticleOptions> getType() {
        return type;
    }
}

