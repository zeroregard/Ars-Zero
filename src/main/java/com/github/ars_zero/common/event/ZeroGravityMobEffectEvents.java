package com.github.ars_zero.common.event;

import com.github.ars_zero.common.gravity.GravitySuppression;
import com.github.ars_zero.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

public final class ZeroGravityMobEffectEvents {
    private ZeroGravityMobEffectEvents() {
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        handleAddition(event);
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        handleRemoval(event);
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        handleRemoval(event);
    }

    private static void handleAddition(MobEffectEvent event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (!isZeroGravity(instance)) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity == null) {
            return;
        }
        if (entity.level().isClientSide) {
            return;
        }
        GravitySuppression.apply(entity, instance.getDuration());
    }

    private static void handleRemoval(MobEffectEvent event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (!isZeroGravity(instance)) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity == null) {
            return;
        }
        if (entity.level().isClientSide) {
            return;
        }
        GravitySuppression.forceRestore(entity);
    }

    private static boolean isZeroGravity(MobEffectInstance instance) {
        if (instance == null) {
            return false;
        }
        return instance.getEffect().is(ModMobEffects.ZERO_GRAVITY.getKey());
    }
}

