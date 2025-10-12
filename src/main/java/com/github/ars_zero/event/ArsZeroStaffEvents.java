package com.github.ars_zero.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.ArsZeroStaff;
import com.github.ars_zero.common.spell.StaffContextRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "ars_zero")
public class ArsZeroStaffEvents {
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Cleanup expired contexts periodically
        StaffContextRegistry.cleanup();
        
        // Call tickPhase for all players using the staff
        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            if (player.getMainHandItem().getItem() instanceof ArsNoitaStaff staff) {
                staff.tickPhase(player, player.getMainHandItem());
            }
        });
    }
}


