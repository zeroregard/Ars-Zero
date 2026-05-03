package com.github.ars_zero.common.world.biome;

import net.minecraft.resources.ResourceLocation;
import terrablender.api.Regions;

/**
 * Isolates all TerraBlender API references so they are only class-loaded when
 * TerraBlender is actually present at runtime.  Call sites must check
 * {@code ModList.get().isLoaded("terrablender")} before invoking any method here.
 */
public class TerraBlenderCompat {

    public static void registerBlightForestRegion(ResourceLocation name, int weight) {
        Regions.register(new BlightForestRegion(name, weight));
    }
}
