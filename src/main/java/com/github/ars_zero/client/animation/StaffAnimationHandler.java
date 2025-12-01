package com.github.ars_zero.client.animation;

import com.github.ars_zero.ArsZero;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * We are temporarily disabling the PAL staff animation controller entirely.
 * This handler explicitly stops any stuck GUI animations.
 */
@EventBusSubscriber(modid = ArsZero.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class StaffAnimationHandler {

    public static final ResourceLocation SPELL_STAFF_LAYER_ID = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff");

    private StaffAnimationHandler() {}

    public static void init() {
        ArsZero.LOGGER.debug("StaffAnimationHandler disabled – explicitly stopping any active animations.");
    }

    public static void onStaffPhase(AbstractClientPlayer player, boolean isMainHand, String phase, int tickCount) {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player instanceof AbstractClientPlayer player) {
            PlayerAnimationController controller = (PlayerAnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(
                player, SPELL_STAFF_LAYER_ID);
            if (controller != null) {
                controller.stop();
            }
        }
    }
}