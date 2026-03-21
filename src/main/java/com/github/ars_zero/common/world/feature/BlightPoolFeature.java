package com.github.ars_zero.common.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.Optional;

/**
 * Places a lake (vanilla LakeFeature) then a single block below the origin (e.g. campfire for smoke).
 * Uses the same fluid and barrier as lava lakes; one block under the pool center gives a campfire.
 */
public class BlightPoolFeature extends Feature<BlightPoolFeature.Configuration> {

    public BlightPoolFeature(Codec<Configuration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Configuration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        Configuration config = context.config();

        // Run vanilla lake at this position
        var lakeKey = ResourceKey.create(Registries.FEATURE, ResourceLocation.parse("minecraft:lake"));
        var lakeOpt = level.registryAccess().registry(Registries.FEATURE).flatMap(r -> r.getHolder(lakeKey));
        if (lakeOpt.isEmpty()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Feature<LakeFeature.Configuration> lake = (Feature<LakeFeature.Configuration>) lakeOpt.get().value();
        LakeFeature.Configuration lakeConfig = new LakeFeature.Configuration(config.fluid(), config.barrier());
        FeaturePlaceContext<LakeFeature.Configuration> lakeContext = new FeaturePlaceContext<>(
            Optional.empty(),
            level,
            context.chunkGenerator(),
            context.random(),
            origin,
            lakeConfig
        );
        if (!lake.place(lakeContext)) {
            return false;
        }

        // Place campfire (or configured block) one block below pool center
        BlockPos below = origin.below();
        if (level.getBlockState(below).canBeReplaced() || level.getBlockState(below).is(Blocks.STONE)) {
            BlockState bottomState = config.belowBlock().getState(context.random(), below);
            level.setBlock(below, bottomState, 3);
        }
        return true;
    }

    public static record Configuration(
        BlockStateProvider fluid,
        BlockStateProvider barrier,
        BlockStateProvider belowBlock
    ) implements FeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                BlockStateProvider.CODEC.fieldOf("fluid").forGetter(Configuration::fluid),
                BlockStateProvider.CODEC.fieldOf("barrier").forGetter(Configuration::barrier),
                BlockStateProvider.CODEC.fieldOf("below_block").forGetter(Configuration::belowBlock)
            ).apply(instance, Configuration::new)
        );
    }
}
