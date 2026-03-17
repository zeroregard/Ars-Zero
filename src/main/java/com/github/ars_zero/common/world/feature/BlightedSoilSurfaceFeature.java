package com.github.ars_zero.common.world.feature;

import com.github.ars_zero.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Replaces grass_block with blighted_soil in a radius around the placement position,
 * but only where a 2D value noise exceeds a threshold. This creates a natural
 * blotchy transition at biome edges — low-noise areas stay as grass_block.
 *
 * The noise uses the same hash function as the client-side BlockColor so the
 * visual green zones roughly correspond to un-replaced grass areas.
 */
public class BlightedSoilSurfaceFeature extends Feature<NoneFeatureConfiguration> {

    public static final Codec<NoneFeatureConfiguration> CODEC = NoneFeatureConfiguration.CODEC;

    // Replace only where noise > this value. 0.35 leaves ~35% of area as grass
    // at the low-noise end, creating irregular green patches at biome edges.
    private static final double REPLACE_THRESHOLD = 0.35;
    private static final double NOISE_SCALE = 1.0 / 64.0;
    private static final long SEED = 0x9E3779B97F4A7C15L;
    private static final int RADIUS = 8;

    public BlightedSoilSurfaceFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        boolean placed = false;

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS) continue;

                int wx = origin.getX() + dx;
                int wz = origin.getZ() + dz;

                double noise = valueNoise2D(wx * NOISE_SCALE, wz * NOISE_SCALE, SEED);
                if (noise < REPLACE_THRESHOLD) continue;

                // Find the surface block at this XZ
                BlockPos surface = new BlockPos(wx, origin.getY(), wz);
                if (level.getBlockState(surface).is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(surface, ModBlocks.BLIGHTED_SOIL.get().defaultBlockState(), 3);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static double valueNoise2D(double x, double z, long seed) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        double fx = x - ix;
        double fz = z - iz;
        double ux = fade(fx);
        double uz = fade(fz);
        double v00 = hash(ix,     iz,     seed);
        double v10 = hash(ix + 1, iz,     seed);
        double v01 = hash(ix,     iz + 1, seed);
        double v11 = hash(ix + 1, iz + 1, seed);
        return lerp(lerp(v00, v10, ux), lerp(v01, v11, ux), uz);
    }

    private static double hash(int ix, int iz, long seed) {
        long h = seed ^ ((long) ix * 0x9E3779B97F4A7C15L) ^ ((long) iz * 0x6C62272E07BB0142L);
        h ^= (h >>> 30);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 31);
        return (double) (h & 0x7FFFFFFFFFFFFFFFL) / (double) Long.MAX_VALUE;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }
}
