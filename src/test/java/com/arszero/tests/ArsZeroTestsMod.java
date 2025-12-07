package com.arszero.tests;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(ArsZeroTestsMod.MOD_ID)
public final class ArsZeroTestsMod {
    static final String MOD_ID = "ars_zero_tests";

    public ArsZeroTestsMod(IEventBus modEventBus) {
        GlobalTestReporter.replaceWith(new FriendlyTestReporter());
        if (TestRegistrationFilter.shouldRegister(WaterVoxelTests.class)) {
            modEventBus.addListener(WaterVoxelTests::registerGameTests);
        }
        if (TestRegistrationFilter.shouldRegister(FireVoxelTests.class)) {
            modEventBus.addListener(FireVoxelTests::registerGameTests);
        }
        if (TestRegistrationFilter.shouldRegister(ArcaneVoxelTests.class)) {
            modEventBus.addListener(ArcaneVoxelTests::registerGameTests);
        }
        if (TestRegistrationFilter.shouldRegister(FireWaterVoxelInteractionBehaviour.class)) {
            modEventBus.addListener(FireWaterVoxelInteractionBehaviour::registerGameTests);
        }
        if (TestRegistrationFilter.shouldRegister(ZeroGravityEffectTests.class)) {
            modEventBus.addListener(ZeroGravityEffectTests::registerGameTests);
        }
        if (TestRegistrationFilter.shouldRegister(MultiphaseSpellTurretTests.class)) {
            modEventBus.addListener(MultiphaseSpellTurretTests::registerGameTests);
        }
        if (TestRegistrationFilter.shouldRegister(AnchorEffectTests.class)) {
            modEventBus.addListener(AnchorEffectTests::registerGameTests);
        }
        modEventBus.addListener(ArsZeroTestsMod::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(TestRegistrationFilter::applyFilterToRegistry);
    }
}

