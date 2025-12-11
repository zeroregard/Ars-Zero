package com.github.ars_zero.client;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.animation.StaffAnimationHandler;
import com.github.ars_zero.client.network.ClientNetworking;
import com.github.ars_zero.client.renderer.entity.ArcaneShieldEntityRenderer;
import com.github.ars_zero.client.renderer.entity.ArcaneVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.BlockGroupEntityRenderer;
import com.github.ars_zero.client.renderer.entity.FireVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.IceVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.LightningVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.StoneVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.WaterVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.WindVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.tile.MultiphaseTurretRenderer;
import com.github.ars_zero.registry.ModBlockEntities;
import com.github.ars_zero.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ArsZeroClient {
    public static void init(IEventBus modEventBus) {
        ArsZero.LOGGER.debug("Initializing Ars Zero client-side components...");
        
        modEventBus.addListener(ArsZeroClient::onClientSetup);
        modEventBus.addListener(ArsZeroClient::registerGuiLayers);
        modEventBus.addListener(ArsZeroClient::registerRenderers);
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
            EntityRenderers.register(ModEntities.STONE_VOXEL_ENTITY.get(), StoneVoxelEntityRenderer::new);
            EntityRenderers.register(ModEntities.WIND_VOXEL_ENTITY.get(), WindVoxelEntityRenderer::new);
            EntityRenderers.register(ModEntities.ICE_VOXEL_ENTITY.get(), IceVoxelEntityRenderer::new);
            EntityRenderers.register(ModEntities.LIGHTNING_VOXEL_ENTITY.get(), LightningVoxelEntityRenderer::new);
            EntityRenderers.register(ModEntities.BLOCK_GROUP.get(), BlockGroupEntityRenderer::new);
            EntityRenderers.register(ModEntities.ARCANE_SHIELD_ENTITY.get(), ArcaneShieldEntityRenderer::new);
        });
    }
    
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MULTIPHASE_SPELL_TURRET.get(), MultiphaseTurretRenderer::new);
    }
    
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
    }
}
