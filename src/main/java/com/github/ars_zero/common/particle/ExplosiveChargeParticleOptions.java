package com.github.ars_zero.common.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ExplosiveChargeParticleOptions implements ParticleOptions {
    
    protected ParticleType<? extends ExplosiveChargeParticleOptions> type;
    
    public static final MapCodec<ExplosiveChargeParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(d -> d.r),
            Codec.FLOAT.fieldOf("g").forGetter(d -> d.g),
            Codec.FLOAT.fieldOf("b").forGetter(d -> d.b)
        )
        .apply(instance, (r, g, b) -> new ExplosiveChargeParticleOptions(null, r, g, b)));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ExplosiveChargeParticleOptions> STREAM_CODEC = StreamCodec.of(
            ExplosiveChargeParticleOptions::toNetwork, ExplosiveChargeParticleOptions::fromNetwork
    );
    
    public static void toNetwork(RegistryFriendlyByteBuf buf, ExplosiveChargeParticleOptions data) {
        buf.writeFloat(data.r);
        buf.writeFloat(data.g);
        buf.writeFloat(data.b);
    }
    
    public static ExplosiveChargeParticleOptions fromNetwork(RegistryFriendlyByteBuf buffer) {
        float r = buffer.readFloat();
        float g = buffer.readFloat();
        float b = buffer.readFloat();
        return new ExplosiveChargeParticleOptions(null, r, g, b);
    }
    
    public float r;
    public float g;
    public float b;
    
    public ExplosiveChargeParticleOptions(float r, float g, float b) {
        this(null, r, g, b);
    }
    
    public ExplosiveChargeParticleOptions(ParticleType<? extends ExplosiveChargeParticleOptions> type, float r, float g, float b) {
        this.type = type;
        this.r = r;
        this.g = g;
        this.b = b;
    }
    
    public void setType(ParticleType<? extends ExplosiveChargeParticleOptions> type) {
        this.type = type;
    }
    
    @Override
    public ParticleType<? extends ExplosiveChargeParticleOptions> getType() {
        return type;
    }
}

