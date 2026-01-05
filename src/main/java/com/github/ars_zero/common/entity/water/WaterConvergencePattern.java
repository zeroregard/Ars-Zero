package com.github.ars_zero.common.entity.water;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WaterConvergencePattern {

    private WaterConvergencePattern() {
    }

    public static List<BlockPos> hemisphereBottomUp(BlockPos sphereCenter, int radius) {
        int r = Math.max(0, radius);
        int r2 = r * r;
        List<BlockPos> positions = new ArrayList<>();

        for (int dy = -r; dy <= 0; dy++) {
            int y = sphereCenter.getY() + dy;
            int dy2 = dy * dy;
            for (int dx = -r; dx <= r; dx++) {
                int dx2 = dx * dx;
                for (int dz = -r; dz <= r; dz++) {
                    int dist2 = dx2 + dz * dz + dy2;
                    if (dist2 > r2) {
                        continue;
                    }
                    int x = sphereCenter.getX() + dx;
                    int z = sphereCenter.getZ() + dz;
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }

        positions.sort(Comparator
                .comparingInt((BlockPos p) -> p.getY())
                .thenComparingInt((BlockPos p) -> p.getX())
                .thenComparingInt((BlockPos p) -> p.getZ()));
        return positions;
    }
}

