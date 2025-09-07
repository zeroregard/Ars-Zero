package com.github.ars_noita.client;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.client.registry.ModKeyBindings;
import net.neoforged.bus.api.IEventBus;

public class ArsNoitaClient {
    public static void init(IEventBus modEventBus) {
        ArsNoita.LOGGER.debug("Initializing Ars Noita client-side components...");
        
        // Register key bindings
        ModKeyBindings.registerKeyBindings(null); // This will be called by the event bus
        
        // Client-side initialization will go here
        // TODO: Register client-side event handlers, etc.
        
        ArsNoita.LOGGER.debug("Ars Noita client initialization completed");
    }
}
