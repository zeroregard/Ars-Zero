package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.world.tree.BigDeadArchwoodTrunkPlacer;
import com.github.ars_zero.common.world.tree.DeadArchwoodTrunkPlacer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Worldgen registries: trunk placer type, configured/placed features, and biome for blight forest.
 */
public final class ModWorldgen {

    public static final DeferredRegister<TrunkPlacerType<?>> TRUNK_PLACER_TYPES =
        DeferredRegister.create(BuiltInRegistries.TRUNK_PLACER_TYPE, ArsZero.MOD_ID);

    public static final DeferredHolder<TrunkPlacerType<?>, TrunkPlacerType<DeadArchwoodTrunkPlacer>> DEAD_ARCHWOOD_TRUNK_PLACER =
        TRUNK_PLACER_TYPES.register("dead_archwood_trunk_placer",
            () -> new TrunkPlacerType<>(DeadArchwoodTrunkPlacer.CODEC));

    public static final DeferredHolder<TrunkPlacerType<?>, TrunkPlacerType<BigDeadArchwoodTrunkPlacer>> BIG_DEAD_ARCHWOOD_TRUNK_PLACER =
        TRUNK_PLACER_TYPES.register("big_dead_archwood_trunk_placer",
            () -> new TrunkPlacerType<>(BigDeadArchwoodTrunkPlacer.CODEC));

    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_DEAD_ARCHWOOD_TREE =
        ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, ArsZero.prefix("dead_archwood_tree"));

    public static final ResourceKey<PlacedFeature> PLACED_DEAD_ARCHWOOD_TREE =
        ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, ArsZero.prefix("placed_dead_archwood_tree"));

    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_BIG_DEAD_ARCHWOOD_TREE =
        ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, ArsZero.prefix("big_dead_archwood_tree"));

    public static final ResourceKey<PlacedFeature> PLACED_BIG_DEAD_ARCHWOOD_TREE =
        ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, ArsZero.prefix("placed_big_dead_archwood_tree"));

    /** Blight forest biome (dead archwood trees, skeletons, no Ars Nouveau critters). */
    public static final ResourceKey<Biome> BLIGHT_FOREST =
        ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, ArsZero.prefix("blight_forest"));
}
