package com.github.ars_zero.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Beneficial effect that grants wither immunity. Applied to the Mage Skeleton's
 * summoned revenant so it doesn't take wither damage or receive the Wither effect.
 * Event handlers in WitherImmunityEffectEvents enforce the immunity.
 */
public class WitherImmunityMobEffect extends MobEffect {

    public WitherImmunityMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3d3d3d);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
