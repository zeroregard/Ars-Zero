package com.github.ars_zero.client.color;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class BlightedSoilBlockColor implements BlockColor {

    public static final BlightedSoilBlockColor INSTANCE = new BlightedSoilBlockColor();

    private static final double NOISE_SCALE = 1.0 / 64.0;
    private static final long SEED = 0x9E3779B97F4A7C15L;

    // Blight green (R=74, G=122, B=48)
    private static final int R0 = 74, G0 = 122, B0 = 48;
    // Ash grey    (R=110, G=112, B=106)
    private static final int R1 = 110, G1 = 112, B1 = 106;

    @Override
    public int getColor(@Nonnull BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        if (tintIndex != 0 || pos == null) {
            return 0xFFFFFFFF;
        }

        float t = (float) valueNoise2D(pos.getX() * NOISE_SCALE, pos.getZ() * NOISE_SCALE, SEED);
        int r = (int) (R0 + t * (R1 - R0));
        int g = (int) (G0 + t * (G1 - G0));
        int b = (int) (B0 + t * (B1 - B0));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
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
