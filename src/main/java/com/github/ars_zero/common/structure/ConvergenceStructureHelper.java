package com.github.ars_zero.common.structure;

import com.github.ars_zero.common.util.BlockProtectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConvergenceStructureHelper {
    public enum Shape {
        CUBE
    }

    public static int minOffset(int size) {
        return -Math.floorDiv(size, 2);
    }

    public static int maxOffset(int size) {
        return minOffset(size) + size - 1;
    }

    public static List<BlockPos> generate(BlockPos center, int size, Shape shape) {
        if (size <= 0) {
            return List.of();
        }

        return switch (shape) {
            case CUBE -> generateCube(center, size);
        };
    }

    public static boolean isSurface(int dx, int dy, int dz, int min, int max, Shape shape) {
        return switch (shape) {
            case CUBE -> dx == min || dx == max || dy == min || dy == max || dz == min || dz == max;
        };
    }

    public static int placeNext(ServerLevel level, List<BlockPos> queue, int startIndex, int blocksPerTick,
            BlockState stateToPlace, @Nullable Player claimActor) {
        if (queue == null || queue.isEmpty()) {
            return 0;
        }

        int index = Math.max(0, startIndex);
        int placed = 0;
        int budget = Math.max(1, blocksPerTick);

        while (placed < budget && index < queue.size()) {
            BlockPos target = queue.get(index);
            index++;

            if (!level.isLoaded(target) || level.isOutsideBuildHeight(target)) {
                continue;
            }

            BlockState existing = level.getBlockState(target);
            if (!existing.canBeReplaced()) {
                continue;
            }

            if (!BlockProtectionUtil.canBlockBePlaced(level, target, stateToPlace, claimActor)) {
                continue;
            }

            level.setBlock(target, stateToPlace, 3);
            placed++;
        }

        return index;
    }

    private static List<BlockPos> generateCube(BlockPos center, int size) {
        int min = minOffset(size);
        int max = maxOffset(size);
        List<BlockPos> result = new ArrayList<>(size * size * size);

        for (int dy = min; dy <= max; dy++) {
            for (int dx = min; dx <= max; dx++) {
                for (int dz = min; dz <= max; dz++) {
                    result.add(center.offset(dx, dy, dz));
                }
            }
        }

        return result;
    }
}

