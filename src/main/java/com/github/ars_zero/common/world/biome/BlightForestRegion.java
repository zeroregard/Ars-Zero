package com.github.ars_zero.common.world.biome;

import com.github.ars_zero.registry.ModWorldgen;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Biomes;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/**
 * Terrablender region that replaces vanilla Forest with blight forest in some chunks.
 * Only Forest is replaced so blight is less frequent; Swamp is left as vanilla.
 */
public class BlightForestRegion extends Region {

    public BlightForestRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addModifiedVanillaOverworldBiomes(mapper, builder ->
            builder.replaceBiome(Biomes.FOREST, ModWorldgen.BLIGHT_FOREST));
    }
}
