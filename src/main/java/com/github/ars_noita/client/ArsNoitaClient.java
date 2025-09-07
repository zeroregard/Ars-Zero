package com.github.ars_noita.client;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.client.registry.ModKeyBindings;
import com.github.ars_noita.client.renderer.entity.VoxelEntityRenderer;
import com.github.ars_noita.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class ArsNoitaClient {
    public static void init(IEventBus modEventBus) {
        ArsNoita.LOGGER.debug("Initializing Ars Noita client-side components...");
        
        // Register key bindings
        ModKeyBindings.registerKeyBindings(null); // This will be called by the event bus
        
        // Register entity renderers
        modEventBus.addListener(ArsNoitaClient::onClientSetup);
        
        ArsNoita.LOGGER.debug("Ars Noita client initialization completed");
    }
    
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(ModEntities.VOXEL_ENTITY.get(), VoxelEntityRenderer::new);
        });
    }
}
