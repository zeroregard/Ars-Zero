package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
    
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, ArsZero.MOD_ID);
    
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLIGHT_SPLASH = PARTICLES.register(
        "blight_splash", 
        () -> new SimpleParticleType(false)
    );
}
