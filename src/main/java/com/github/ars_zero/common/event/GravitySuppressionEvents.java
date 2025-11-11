package com.github.ars_zero.common.event;

import com.github.ars_zero.common.gravity.GravitySuppression;
import com.github.ars_zero.registry.ModAttachments;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class GravitySuppressionEvents {
    private GravitySuppressionEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!entity.getData(ModAttachments.GRAVITY_SUPPRESSION).isActive()) {
            return;
        }
        GravitySuppression.tickPre(entity);
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!entity.getData(ModAttachments.GRAVITY_SUPPRESSION).isActive()) {
            return;
        }
        GravitySuppression.tickPost(entity);
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (!event.getLevel().isClientSide && entity.getData(ModAttachments.GRAVITY_SUPPRESSION).isActive()) {
            GravitySuppression.forceRestore(entity);
        }
    }
}

