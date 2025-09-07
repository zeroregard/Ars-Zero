package com.github.ars_noita.client;

import com.github.ars_noita.ArsNoita;
import net.neoforged.bus.api.IEventBus;

public class ArsNoitaClient {
    public static void init(IEventBus modEventBus) {
        ArsNoita.LOGGER.debug("Initializing Ars Noita client-side components...");
        
        // Client-side initialization will go here
        // TODO: Register client-side event handlers, key bindings, etc.
        
        ArsNoita.LOGGER.debug("Ars Noita client initialization completed");
    }
}
