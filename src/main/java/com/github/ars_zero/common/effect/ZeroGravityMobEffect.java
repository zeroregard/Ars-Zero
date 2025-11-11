package com.github.ars_zero.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ZeroGravityMobEffect extends MobEffect {
    public ZeroGravityMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7ED6FF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}