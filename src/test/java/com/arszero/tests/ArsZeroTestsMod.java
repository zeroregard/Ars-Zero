package com.arszero.tests;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ArsZeroTestsMod.MOD_ID)
public final class ArsZeroTestsMod {
    static final String MOD_ID = "ars_zero_tests";

    public ArsZeroTestsMod(IEventBus modEventBus) {
        modEventBus.addListener(WaterVoxelTests::registerGameTests);
        modEventBus.addListener(FireVoxelTests::registerGameTests);
        modEventBus.addListener(ArcaneVoxelTests::registerGameTests);
    }
}

