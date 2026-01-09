package com.github.ars_zero.common.entity.water;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class WaterConvergencePattern {

    private WaterConvergencePattern() {
    }

    public static List<BlockPos> hemisphereBottomUp(BlockPos sphereCenter, int radius) {
        int r = Math.max(0, radius);
        int r2 = r * r;
        List<BlockPos> positions = new ArrayList<>();

        for (int dy = -r; dy <= -2; dy++) {
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
                .thenComparingInt((BlockPos p) -> {
                    int y = p.getY();
                    return (y % 2 == 0) ? p.getX() : -p.getX();
                })
                .thenComparingInt((BlockPos p) -> p.getZ()));
        return positions;
    }

    public static List<BlockPos> floodFillFloorByFloor(ServerLevel level, BlockPos startPos, int radius) {
        List<BlockPos> positions = new ArrayList<>();
        int r = Math.max(0, radius);

        int startY = startPos.getY();
        int minY = startY - r;
        int maxY = startY + r;

        for (int currentY = minY; currentY <= maxY; currentY++) {
            List<BlockPos> floorPositions = floodFillFloor(level, startPos, currentY, r);
            positions.addAll(floorPositions);
        }

        return positions;
    }

    public static List<BlockPos> floodFillFloor(ServerLevel level, BlockPos centerPos, int y, int radius) {
        int centerX = centerPos.getX();
        int centerZ = centerPos.getZ();
        int radiusSquared = radius * radius;
        List<BlockPos> positions = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        BlockPos startPos = new BlockPos(centerX, y, centerZ);
        if (isFillable(level, startPos, centerX, centerZ, radiusSquared)) {
            queue.add(startPos);
            visited.add(startPos);
        }

        int[] dxOffsets = { 0, 1, 0, -1 };
        int[] dzOffsets = { 1, 0, -1, 0 };

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            positions.add(current);

            for (int i = 0; i < 4; i++) {
                int newX = current.getX() + dxOffsets[i];
                int newZ = current.getZ() + dzOffsets[i];
                BlockPos neighbor = new BlockPos(newX, y, newZ);

                if (visited.contains(neighbor)) {
                    continue;
                }

                if (isFillable(level, neighbor, centerX, centerZ, radiusSquared)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return positions;
    }

    private static boolean isFillable(ServerLevel level, BlockPos pos, int centerX, int centerZ, int radiusSquared) {
        if (!level.isLoaded(pos)) {
            return false;
        }

        int dx = pos.getX() - centerX;
        int dz = pos.getZ() - centerZ;
        int horizontalDist2 = dx * dx + dz * dz;

        if (horizontalDist2 > radiusSquared) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        return state.isAir();
    }
}
