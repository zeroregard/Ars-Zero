package com.github.ars_zero.client;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.animation.StaffAnimationHandler;
import com.github.ars_zero.client.network.ClientNetworking;
import com.github.ars_zero.client.registry.ModKeyBindings;
import com.github.ars_zero.client.renderer.entity.ArcaneVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.BlockGroupEntityRenderer;
import com.github.ars_zero.client.renderer.entity.FireVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.WaterVoxelEntityRenderer;
import com.github.ars_zero.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ArsZeroClient {
    public static void init(IEventBus modEventBus) {
        ArsZero.LOGGER.debug("Initializing Ars Zero client-side components...");
        
        modEventBus.addListener(ArsZeroClient::onClientSetup);
        modEventBus.addListener(ArsZeroClient::registerGuiLayers);
        modEventBus.addListener(ClientNetworking::register);
        
        NeoForge.EVENT_BUS.register(StaffScrollHandler.class);
        
        StaffAnimationHandler.init();
        
        ArsZero.LOGGER.debug("Ars Zero client initialization completed");
    }
    
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(ModEntities.ARCANE_VOXEL_ENTITY.get(), ArcaneVoxelEntityRenderer::new);
            EntityRenderers.register(ModEntities.WATER_VOXEL_ENTITY.get(), WaterVoxelEntityRenderer::new);
            EntityRenderers.register(ModEntities.FIRE_VOXEL_ENTITY.get(), FireVoxelEntityRenderer::new);
            EntityRenderers.register(ModEntities.BLOCK_GROUP.get(), BlockGroupEntityRenderer::new);
        });
    }
    
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // event.registerAbove(VanillaGuiLayers.CROSSHAIR, ArsZero.prefix("staff_hud"), GuiStaffHUD.OVERLAY);
    }
}
