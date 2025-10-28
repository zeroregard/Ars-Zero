package com.github.ars_zero.client;

import com.github.ars_zero.common.item.ArsZeroStaff;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.common.network.PacketAdjustStaffDistance;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;

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
        
        boolean usingStaffInMainHand = mainHandItem.getItem() instanceof ArsZeroStaff && player.isUsingItem() && player.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND;
        boolean usingStaffInOffHand = offHandItem.getItem() instanceof ArsZeroStaff && player.isUsingItem() && player.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND;
        
        if (usingStaffInMainHand || usingStaffInOffHand) {
            event.setCanceled(true);
            Networking.sendToServer(new PacketAdjustStaffDistance(event.getScrollDeltaY()));
        }
    }
}

