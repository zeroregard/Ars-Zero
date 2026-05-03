package com.github.ars_zero.common.world.biome;

import com.github.ars_zero.registry.ModWorldgen;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.ParameterUtils;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/**
 * Terrablender region that adds blight forest only in the "interior" of the forest climate zone
 * (narrow parameter band), so it appears in larger patches instead of narrow strips between other biomes.
 * Restricted to a single climate slot (COOL + WET + FAR_INLAND + EROSION_5) to keep it rare —
 * roughly 1 blight forest per 5 archwood forests.
 */
public class BlightForestRegion extends Region {

    /** Weirdness: narrowest band around 0, same as swamp (only the flattest possible terrain). */
    private static final float FLAT_WEIRDNESS = 0.03F;

    public BlightForestRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        // Single climate combination to keep blight forest rare. Previously used 2×2×2 = 8 combinations
        // which caused it to appear far more often than the archwood forests.
        new ParameterUtils.ParameterPointListBuilder()
            .temperature(ParameterUtils.Temperature.COOL)
            .humidity(ParameterUtils.Humidity.WET)
            .continentalness(ParameterUtils.Continentalness.FAR_INLAND)
            .erosion(ParameterUtils.Erosion.EROSION_5)
            .depth(ParameterUtils.Depth.SURFACE)
            .weirdness(Climate.Parameter.span(-FLAT_WEIRDNESS, FLAT_WEIRDNESS))
            .build()
            .forEach(point -> mapper.accept(new Pair<>(point, ModWorldgen.BLIGHT_FOREST)));
    }
}
