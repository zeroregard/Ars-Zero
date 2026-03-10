package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.world.feature.BlightPoolFeature;
import com.github.ars_zero.common.world.feature.BlightedSoilSurfaceFeature;
import com.github.ars_zero.common.world.structure.BlightDungeonStructure;
import com.github.ars_zero.common.world.structure.CobwebProcessor;
import com.github.ars_zero.common.world.structure.StripWaterloggedProcessor;
import com.github.ars_zero.common.world.placement.NoBlightLogNearbyFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import com.github.ars_zero.common.world.tree.BigDeadArchwoodTrunkPlacer;
import com.github.ars_zero.common.world.tree.DeadArchwoodTrunkPlacer;
import com.github.ars_zero.common.world.tree.FlatBlobFoliagePlacer;
import com.github.ars_zero.common.world.tree.HugeDeadArchwoodTrunkPlacer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Worldgen registries: trunk placer type, features, configured/placed features, and biome for blight forest.
 */
public final class ModWorldgen {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
        DeferredRegister.create(BuiltInRegistries.STRUCTURE_TYPE, ArsZero.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<BlightDungeonStructure>> NECROPOLIS_STRUCTURE =
        STRUCTURE_TYPES.register("necropolis", () -> () -> BlightDungeonStructure.CODEC);

    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSOR_TYPES =
        DeferredRegister.create(BuiltInRegistries.STRUCTURE_PROCESSOR, ArsZero.MOD_ID);

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<CobwebProcessor>> COBWEB_PROCESSOR =
        STRUCTURE_PROCESSOR_TYPES.register("cobweb", () -> () -> CobwebProcessor.CODEC);

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<StripWaterloggedProcessor>> STRIP_WATERLOGGED_PROCESSOR =
        STRUCTURE_PROCESSOR_TYPES.register("strip_waterlogged", () -> () -> StripWaterloggedProcessor.CODEC);

    public static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(BuiltInRegistries.FEATURE, ArsZero.MOD_ID);

    public static final DeferredHolder<Feature<?>, BlightPoolFeature> BLIGHT_POOL_FEATURE =
        FEATURES.register("blight_pool", () -> new BlightPoolFeature(BlightPoolFeature.Configuration.CODEC));

    public static final DeferredHolder<Feature<?>, BlightedSoilSurfaceFeature> BLIGHTED_SOIL_SURFACE_FEATURE =
        FEATURES.register("blighted_soil_surface", () -> new BlightedSoilSurfaceFeature(BlightedSoilSurfaceFeature.CODEC));

    public static final DeferredRegister<TrunkPlacerType<?>> TRUNK_PLACER_TYPES =
        DeferredRegister.create(BuiltInRegistries.TRUNK_PLACER_TYPE, ArsZero.MOD_ID);

    public static final DeferredHolder<TrunkPlacerType<?>, TrunkPlacerType<DeadArchwoodTrunkPlacer>> DEAD_ARCHWOOD_TRUNK_PLACER =
        TRUNK_PLACER_TYPES.register("dead_archwood_trunk_placer",
            () -> new TrunkPlacerType<>(DeadArchwoodTrunkPlacer.CODEC));

    public static final DeferredHolder<TrunkPlacerType<?>, TrunkPlacerType<BigDeadArchwoodTrunkPlacer>> BIG_DEAD_ARCHWOOD_TRUNK_PLACER =
        TRUNK_PLACER_TYPES.register("big_dead_archwood_trunk_placer",
            () -> new TrunkPlacerType<>(BigDeadArchwoodTrunkPlacer.CODEC));

    public static final DeferredHolder<TrunkPlacerType<?>, TrunkPlacerType<HugeDeadArchwoodTrunkPlacer>> HUGE_DEAD_ARCHWOOD_TRUNK_PLACER =
        TRUNK_PLACER_TYPES.register("huge_dead_archwood_trunk_placer",
            () -> new TrunkPlacerType<>(HugeDeadArchwoodTrunkPlacer.CODEC));

    public static final DeferredRegister<FoliagePlacerType<?>> FOLIAGE_PLACER_TYPES =
        DeferredRegister.create(BuiltInRegistries.FOLIAGE_PLACER_TYPE, ArsZero.MOD_ID);

    public static final DeferredHolder<FoliagePlacerType<?>, FoliagePlacerType<FlatBlobFoliagePlacer>> FLAT_BLOB_FOLIAGE_PLACER =
        FOLIAGE_PLACER_TYPES.register("flat_blob_foliage_placer",
            () -> new FoliagePlacerType<>(FlatBlobFoliagePlacer.CODEC));

    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIER_TYPES =
        DeferredRegister.create(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, ArsZero.MOD_ID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<NoBlightLogNearbyFilter>> NO_BLIGHT_LOG_NEARBY_FILTER =
        PLACEMENT_MODIFIER_TYPES.register("no_blight_log_nearby",
            () -> new PlacementModifierType<NoBlightLogNearbyFilter>() {
                @Override
                public com.mojang.serialization.MapCodec<NoBlightLogNearbyFilter> codec() {
                    return NoBlightLogNearbyFilter.CODEC;
                }
            });

    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_DEAD_ARCHWOOD_TREE =
        ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, ArsZero.prefix("dead_archwood_tree"));

    public static final ResourceKey<PlacedFeature> PLACED_DEAD_ARCHWOOD_TREE =
        ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, ArsZero.prefix("placed_dead_archwood_tree"));

    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_BIG_DEAD_ARCHWOOD_TREE =
        ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, ArsZero.prefix("big_dead_archwood_tree"));

    public static final ResourceKey<PlacedFeature> PLACED_BIG_DEAD_ARCHWOOD_TREE =
        ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, ArsZero.prefix("placed_big_dead_archwood_tree"));

    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_HUGE_DEAD_ARCHWOOD_TREE =
        ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, ArsZero.prefix("huge_dead_archwood_tree"));

    public static final ResourceKey<PlacedFeature> PLACED_HUGE_DEAD_ARCHWOOD_TREE =
        ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, ArsZero.prefix("placed_huge_dead_archwood_tree"));

    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_PATCH_DEAD_BUSH =
        ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, ArsZero.prefix("patch_dead_bush"));
    public static final ResourceKey<PlacedFeature> PLACED_PATCH_DEAD_BUSH =
        ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, ArsZero.prefix("placed_patch_dead_bush"));

    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_DISK_COARSE_DIRT =
        ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, ArsZero.prefix("disk_coarse_dirt"));
    public static final ResourceKey<PlacedFeature> PLACED_DISK_COARSE_DIRT =
        ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, ArsZero.prefix("placed_disk_coarse_dirt"));

    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_DISK_GRAVEL_SURFACE =
        ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, ArsZero.prefix("disk_gravel_surface"));
    public static final ResourceKey<PlacedFeature> PLACED_DISK_GRAVEL_SURFACE =
        ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, ArsZero.prefix("placed_disk_gravel_surface"));

    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_BLIGHT_POOL =
        ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, ArsZero.prefix("blight_pool"));
    public static final ResourceKey<PlacedFeature> PLACED_BLIGHT_POOL =
        ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, ArsZero.prefix("placed_blight_pool"));

    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_PATCH_GRASS =
        ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, ArsZero.prefix("patch_grass"));
    public static final ResourceKey<PlacedFeature> PLACED_PATCH_GRASS =
        ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, ArsZero.prefix("placed_patch_grass"));

    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_REPLACE_GRASS_WITH_BLIGHTED_SOIL =
        ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, ArsZero.prefix("replace_grass_with_blighted_soil"));
    public static final ResourceKey<PlacedFeature> PLACED_REPLACE_GRASS_WITH_BLIGHTED_SOIL =
        ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, ArsZero.prefix("placed_replace_grass_with_blighted_soil"));

    /** Blight forest biome (dead archwood trees, skeletons, no Ars Nouveau critters). */
    public static final ResourceKey<Biome> BLIGHT_FOREST =
        ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, ArsZero.prefix("blight_forest"));
}
