package com.github.ars_zero.client;

import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.common.network.PacketScrollMultiPhaseDevice;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

public class StaffScrollHandler {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) {
            return;
        }

        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();

        boolean usingStaffInMainHand = mainHandItem.getItem() instanceof AbstractSpellStaff && player.isUsingItem()
                && player.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND;
        boolean usingStaffInOffHand = offHandItem.getItem() instanceof AbstractSpellStaff && player.isUsingItem()
                && player.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND;

        if (usingStaffInMainHand || usingStaffInOffHand) {
            event.setCanceled(true);
            boolean modifierHeld = isModifierKeyDown(mc);
            Networking.sendToServer(new PacketScrollMultiPhaseDevice(event.getScrollDeltaY(), modifierHeld));
        }
    }

    private static boolean isModifierKeyDown(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT) ||
                InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT) ||
                InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SUPER) ||
                InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SUPER);
    }
}
