package com.github.ars_zero.client;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.gui.GuiStaffHUD;
import com.github.ars_zero.client.registry.ModKeyBindings;
import com.github.ars_zero.client.renderer.entity.VoxelEntityRenderer;
import com.github.ars_zero.client.renderer.StaffDebugHUD;
import com.github.ars_zero.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

public class ArsZeroClient {
    public static void init(IEventBus modEventBus) {
        ArsZero.LOGGER.debug("Initializing Ars Zero client-side components...");
        
        ModKeyBindings.registerKeyBindings(null);
        
        modEventBus.addListener(ArsZeroClient::onClientSetup);
        modEventBus.addListener(ArsZeroClient::registerGuiLayers);
        
        NeoForge.EVENT_BUS.register(StaffDebugHUD.class);
        
        ArsZero.LOGGER.debug("Ars Zero client initialization completed");
    }
    
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(ModEntities.VOXEL_ENTITY.get(), VoxelEntityRenderer::new);
        });
    }
    
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ArsZero.prefix("staff_hud"), GuiStaffHUD.OVERLAY);
    }
}
