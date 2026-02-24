package com.github.ars_zero.common.datagen;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.world.tree.BigDeadArchwoodTrunkPlacer;
import com.github.ars_zero.common.world.tree.DeadArchwoodTrunkPlacer;
import com.github.ars_zero.registry.ModBlocks;
import com.github.ars_zero.registry.ModWorldgen;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.sounds.Musics;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class WorldgenProvider extends DatapackBuiltinEntriesProvider {

    private static final net.minecraft.core.RegistrySetBuilder BUILDER = new net.minecraft.core.RegistrySetBuilder()
        .add(Registries.CONFIGURED_FEATURE, WorldgenProvider::bootstrapConfiguredFeatures)
        .add(Registries.PLACED_FEATURE, WorldgenProvider::bootstrapPlacedFeatures)
        .add(Registries.BIOME, WorldgenProvider::bootstrapBiomes);

    public WorldgenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(ArsZero.MOD_ID));
    }

    private static void bootstrapConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        TreeConfiguration deadTreeConfig = new TreeConfiguration.TreeConfigurationBuilder(
            BlockStateProvider.simple(ModBlocks.BLIGHT_ARCHWOOD_LOG.get()),
            new DeadArchwoodTrunkPlacer(9, 1, 0),
            BlockStateProvider.simple(Blocks.AIR),
            new BlobFoliagePlacer(UniformInt.of(0, 0), UniformInt.of(0, 0), 0),
            new TwoLayersFeatureSize(2, 0, 2)
        ).build();

        context.register(ModWorldgen.CONFIGURED_DEAD_ARCHWOOD_TREE,
            new ConfiguredFeature<>(Feature.TREE, deadTreeConfig));

        TreeConfiguration bigDeadTreeConfig = new TreeConfiguration.TreeConfigurationBuilder(
            BlockStateProvider.simple(ModBlocks.BLIGHT_ARCHWOOD_LOG.get()),
            new BigDeadArchwoodTrunkPlacer(16, 4, 4),
            BlockStateProvider.simple(Blocks.AIR),
            new BlobFoliagePlacer(UniformInt.of(0, 0), UniformInt.of(0, 0), 0),
            new TwoLayersFeatureSize(2, 0, 2)
        ).build();

        context.register(ModWorldgen.CONFIGURED_BIG_DEAD_ARCHWOOD_TREE,
            new ConfiguredFeature<>(Feature.TREE, bigDeadTreeConfig));
    }

    private static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        context.register(ModWorldgen.PLACED_DEAD_ARCHWOOD_TREE,
            new PlacedFeature(
                features.getOrThrow(ModWorldgen.CONFIGURED_DEAD_ARCHWOOD_TREE),
                VegetationPlacements.treePlacement(RarityFilter.onAverageOnceEvery(1))
            ));
        context.register(ModWorldgen.PLACED_BIG_DEAD_ARCHWOOD_TREE,
            new PlacedFeature(
                features.getOrThrow(ModWorldgen.CONFIGURED_BIG_DEAD_ARCHWOOD_TREE),
                VegetationPlacements.treePlacement(RarityFilter.onAverageOnceEvery(4))
            ));
    }

    private static void bootstrapBiomes(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        BiomeDefaultFeatures.addDefaultSprings(generation);
        BiomeDefaultFeatures.addSurfaceFreezing(generation);
        // No flowers in blight forest
        BiomeDefaultFeatures.addFerns(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addExtraGold(generation);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MiscOverworldPlacements.DISK_SAND);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MiscOverworldPlacements.DISK_CLAY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MiscOverworldPlacements.DISK_GRAVEL);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placedFeatures.getOrThrow(ModWorldgen.PLACED_DEAD_ARCHWOOD_TREE));
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placedFeatures.getOrThrow(ModWorldgen.PLACED_BIG_DEAD_ARCHWOOD_TREE));
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation);

        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
        spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.SKELETON, 100, 4, 4));
        BiomeDefaultFeatures.commonSpawns(spawns);

        // Flatter (swamp-like) terrain would require overworld surface rules conditioned on ars_zero:blight_forest.
        // NeoForge's built-in biome modifiers do not expose surface rules; dimension/chunk-gen modification would be needed. Left as follow-up.
        context.register(ModWorldgen.BLIGHT_FOREST, new Biome.BiomeBuilder()
            .hasPrecipitation(true)
            .downfall(0.9f)
            .temperature(0.65f)
            .generationSettings(generation.build())
            .mobSpawnSettings(spawns.build())
            .specialEffects(new BiomeSpecialEffects.Builder()
                .waterColor(0x2d5016)
                .waterFogColor(0x1a3009)
                .fogColor(0x303030)
                .skyColor(0x303030)
                .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.DARK_FOREST)
                .grassColorOverride(0x5B6656)
                .foliageColorOverride(0x5B6656)
                .ambientMoodSound(net.minecraft.world.level.biome.AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                .backgroundMusic(Musics.createGameMusic(net.minecraft.sounds.SoundEvents.MUSIC_BIOME_FOREST)).build())
            .build());
    }
}
