package com.github.ars_noita.event;

import com.github.ars_noita.common.spell.StaffContextRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "ars_noita")
public class ArsNoitaStaffEvents {
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Cleanup expired contexts periodically
        StaffContextRegistry.cleanup();
    }
}


