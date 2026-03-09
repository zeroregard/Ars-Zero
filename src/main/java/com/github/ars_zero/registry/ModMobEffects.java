package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.effect.WitherImmunityMobEffect;
import com.github.ars_zero.common.effect.ZeroGravityMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {
    private ModMobEffects() {
    }

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ArsZero.MOD_ID);

    public static final DeferredHolder<MobEffect, ZeroGravityMobEffect> ZERO_GRAVITY = MOB_EFFECTS.register(
        "zero_gravity",
        ZeroGravityMobEffect::new
    );

    public static final DeferredHolder<MobEffect, WitherImmunityMobEffect> WITHER_IMMUNITY = MOB_EFFECTS.register(
        "wither_immunity",
        WitherImmunityMobEffect::new
    );
}


