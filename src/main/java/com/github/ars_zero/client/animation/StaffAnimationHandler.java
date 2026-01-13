package com.github.ars_zero.client.animation;

import com.github.ars_zero.ArsZero;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ArsZero.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class StaffAnimationHandler {

    public static final ResourceLocation SPELL_STAFF_LAYER_ID = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff");

    private StaffAnimationHandler() {}

    public static void init() {
        ArsZero.LOGGER.debug("StaffAnimationHandler disabled.");
    }

    public static void onStaffPhase(AbstractClientPlayer player, boolean isMainHand, String phase, int tickCount) {
    }
}
