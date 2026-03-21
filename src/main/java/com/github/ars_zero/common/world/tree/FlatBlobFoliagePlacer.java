package com.github.ars_zero.common.world.tree;

import com.github.ars_zero.registry.ModWorldgen;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

/**
 * Wide, flat canopy foliage placer for dead archwood trees.
 * Places leaves in disc layers — much wider than tall — so nearby trees
 * have overlapping leaf canopies that connect horizontally.
 *
 * layerCount: number of vertical leaf layers (stacked downward from attachment)
 * outerRadius: extra radius added to the widest (bottom) layer for a flared skirt
 */
public class FlatBlobFoliagePlacer extends FoliagePlacer {

    private final int layerCount;
    private final int outerRadius;

    public static final MapCodec<FlatBlobFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(builder ->
        foliagePlacerParts(builder)
            .and(com.mojang.serialization.Codec.intRange(1, 6)
                .fieldOf("layer_count").forGetter(p -> p.layerCount))
            .and(com.mojang.serialization.Codec.intRange(0, 8)
                .fieldOf("outer_radius").forGetter(p -> p.outerRadius))
            .apply(builder, FlatBlobFoliagePlacer::new));

    public FlatBlobFoliagePlacer(IntProvider radius, IntProvider offset, int layerCount, int outerRadius) {
        super(radius, offset);
        this.layerCount = layerCount;
        this.outerRadius = outerRadius;
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return ModWorldgen.FLAT_BLOB_FOLIAGE_PLACER.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter setter, RandomSource random,
                                  TreeConfiguration config, int maxFreeTreeHeight,
                                  FoliageAttachment attachment, int foliageHeight, int foliageRadius, int offset) {
        for (int layer = 0; layer < layerCount; layer++) {
            // Top layer = base radius, bottom layer = base radius + outerRadius flare
            int layerRadius;
            if (layerCount <= 1) {
                layerRadius = foliageRadius + outerRadius;
            } else {
                layerRadius = foliageRadius + (outerRadius * layer / (layerCount - 1));
            }

            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    // Circular disc with soft randomised edge
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    double edgeNoise = random.nextDouble() * 0.9;
                    if (dist <= layerRadius - edgeNoise) {
                        tryPlaceLeaf(level, setter, random, config,
                            attachment.pos().offset(dx, -layer, dz));
                    }
                }
            }
        }
    }

    @Override
    public int foliageHeight(RandomSource random, int height, TreeConfiguration config) {
        return layerCount;
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int dx, int dy, int dz, int range, boolean doubleTrunk) {
        // All placement is handled in createFoliage — skip the default blob logic
        return true;
    }
}
