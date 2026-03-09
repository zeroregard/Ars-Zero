package com.github.ars_zero.common.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.registry.ModMobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

@EventBusSubscriber(modid = ArsZero.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class WitherImmunityEffectEvents {

    private WitherImmunityEffectEvents() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!event.getEntity().hasEffect(ModMobEffects.WITHER_IMMUNITY)) {
            return;
        }
        if (!event.getSource().is(DamageTypes.WITHER) && !event.getSource().is(DamageTypes.WITHER_SKULL)) {
            return;
        }
        event.getContainer().setNewDamage(0);
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!event.getEntity().hasEffect(ModMobEffects.WITHER_IMMUNITY)) {
            return;
        }
        if (event.getEffectInstance().getEffect().value() != MobEffects.WITHER) {
            return;
        }
        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
}
