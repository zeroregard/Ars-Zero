package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.world.feature.BlightPoolFeature;
import com.github.ars_zero.common.world.placement.NoBlightLogNearbyFilter;
import com.github.ars_zero.common.world.tree.BigDeadArchwoodTrunkPlacer;
import com.github.ars_zero.common.world.tree.DeadArchwoodTrunkPlacer;
import com.github.ars_zero.common.world.tree.HugeDeadArchwoodTrunkPlacer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Worldgen registries: trunk placer type, features, configured/placed features, and biome for blight forest.
 */
public final class ModWorldgen {

    public static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(BuiltInRegistries.FEATURE, ArsZero.MOD_ID);

    public static final DeferredHolder<Feature<?>, BlightPoolFeature> BLIGHT_POOL_FEATURE =
        FEATURES.register("blight_pool", () -> new BlightPoolFeature(BlightPoolFeature.Configuration.CODEC));

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

    /** Blight forest biome (dead archwood trees, skeletons, no Ars Nouveau critters). */
    public static final ResourceKey<Biome> BLIGHT_FOREST =
        ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, ArsZero.prefix("blight_forest"));
}
