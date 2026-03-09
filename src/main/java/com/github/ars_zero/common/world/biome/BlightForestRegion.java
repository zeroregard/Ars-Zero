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
import terrablender.api.VanillaParameterOverlayBuilder;

import java.util.function.Consumer;

/**
 * Terrablender region that adds blight forest only in the "interior" of the forest climate zone
 * (narrow parameter band), so it appears in larger patches instead of narrow strips between other biomes.
 * Excludes ocean/coast (mid–far inland only). Restricts to swamp-level flatness only: narrowest weirdness ±0.03 and EROSION_5 only.
 */
public class BlightForestRegion extends Region {

    /** Weirdness: narrowest band around 0, same order as swamp (only the flattest possible terrain). */
    private static final float FLAT_WEIRDNESS = 0.03F;

    public BlightForestRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        VanillaParameterOverlayBuilder builder = new VanillaParameterOverlayBuilder();

        // Continentalness: MID_INLAND–FAR_INLAND so we never get ocean or coast.
        // Erosion: EROSION_5 only (flattest band, same idea as swamp). Weirdness ±0.03 = narrowest flat band.
        new ParameterUtils.ParameterPointListBuilder()
            .temperature(ParameterUtils.Temperature.COOL, ParameterUtils.Temperature.NEUTRAL)
            .humidity(ParameterUtils.Humidity.WET, ParameterUtils.Humidity.HUMID)
            .continentalness(ParameterUtils.Continentalness.MID_INLAND, ParameterUtils.Continentalness.FAR_INLAND)
            .erosion(ParameterUtils.Erosion.EROSION_5)
            .depth(ParameterUtils.Depth.SURFACE)
            .weirdness(Climate.Parameter.span(-FLAT_WEIRDNESS, FLAT_WEIRDNESS))
            .build()
            .forEach(point -> builder.add(point, ModWorldgen.BLIGHT_FOREST));

        builder.build().forEach(mapper);
    }
}
