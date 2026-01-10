package com.github.ars_zero.client;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.animation.StaffAnimationHandler;
import com.github.ars_zero.client.gui.GuiStaffHUD;
import com.github.ars_zero.client.network.ClientNetworking;
import com.github.ars_zero.client.renderer.entity.ArcaneVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.BlockGroupEntityRenderer;
import com.github.ars_zero.client.renderer.entity.FireVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.IceVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.LightningVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.BlightVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.StoneVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.WaterVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.WindVoxelEntityRenderer;
import com.github.ars_zero.client.renderer.entity.ConjureTerrainConvergenceEntityRenderer;
import com.github.ars_zero.client.renderer.entity.ExplosionControllerEntityRenderer;
import com.github.ars_zero.client.renderer.entity.ExplosionBurstProjectileRenderer;
import com.github.ars_zero.client.renderer.entity.WaterConvergenceControllerEntityRenderer;
import com.github.ars_zero.client.renderer.entity.SourceJarChargerEntityRenderer;
import com.github.ars_zero.client.renderer.entity.PlayerChargerEntityRenderer;
import com.github.ars_zero.client.particle.BlightSplashParticle;
import com.github.ars_zero.client.particle.ExplosiveChargeParticle;
import com.github.ars_zero.client.particle.FastPoofParticle;
import com.github.ars_zero.client.particle.SourceJarChargeParticle;
import com.github.ars_zero.client.renderer.tile.MultiphaseTurretRenderer;
import com.github.ars_zero.registry.ModBlockEntities;
import com.github.ars_zero.registry.ModEntities;
import com.github.ars_zero.registry.ModParticles;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

public class ArsZeroClient {
    public static void init(IEventBus modEventBus) {
        ArsZero.LOGGER.debug("Initializing Ars Zero client-side components...");

        modEventBus.addListener(ArsZeroClient::onClientSetup);
        modEventBus.addListener(ArsZeroClient::registerGuiLayers);
        modEventBus.addListener(ArsZeroClient::registerRenderers);
        modEventBus.addListener(ArsZeroClient::registerParticleProviders);
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
            EntityRenderers.register(ModEntities.BLIGHT_VOXEL_ENTITY.get(), BlightVoxelEntityRenderer::new);
            EntityRenderers.register(ModEntities.BLOCK_GROUP.get(), BlockGroupEntityRenderer::new);
            EntityRenderers.register(ModEntities.EXPLOSION_CONTROLLER.get(), ExplosionControllerEntityRenderer::new);
            EntityRenderers.register(ModEntities.WATER_CONVERGENCE_CONTROLLER.get(),
                    WaterConvergenceControllerEntityRenderer::new);
            EntityRenderers.register(ModEntities.CONJURE_TERRAIN_CONVERGENCE_CONTROLLER.get(),
                    ConjureTerrainConvergenceEntityRenderer::new);
            EntityRenderers.register(ModEntities.EXPLOSION_BURST_PROJECTILE.get(),
                    ExplosionBurstProjectileRenderer::new);
            EntityRenderers.register(ModEntities.SOURCE_JAR_CHARGER.get(), SourceJarChargerEntityRenderer::new);
            EntityRenderers.register(ModEntities.PLAYER_CHARGER.get(), PlayerChargerEntityRenderer::new);
        });
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MULTIPHASE_SPELL_TURRET.get(),
                MultiphaseTurretRenderer::new);
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ArsZero.prefix("staff_hud"), GuiStaffHUD.OVERLAY);
    }

    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.BLIGHT_SPLASH.get(), BlightSplashParticle.Provider::new);
        event.registerSpriteSet(ModParticles.EXPLOSIVE_CHARGE.get(), ExplosiveChargeParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SOURCE_JAR_CHARGE.get(), SourceJarChargeParticle.Provider::new);
        event.registerSpriteSet(ModParticles.FAST_POOF.get(), FastPoofParticle.Provider::new);
    }
}
