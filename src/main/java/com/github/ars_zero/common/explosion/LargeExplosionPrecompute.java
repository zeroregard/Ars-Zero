package com.github.ars_zero.common.explosion;

import com.github.ars_zero.common.util.BlockImmutabilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class LargeExplosionPrecompute {
    private LargeExplosionPrecompute() {
    }

    public static ExplosionWorkList compute(Level level, BlockPos origin, double radius) {
        int rInt = (int) Math.ceil(radius);
        double rSq = radius * radius;

        int initialCapacity = estimateInitialCapacity(radius);
        ExplosionWorkList list = new ExplosionWorkList(initialCapacity);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = -rInt; dy <= rInt; dy++) {
            int y = origin.getY() + dy;
            for (int dx = -rInt; dx <= rInt; dx++) {
                int x = origin.getX() + dx;
                for (int dz = -rInt; dz <= rInt; dz++) {
                    int dist2 = dx * dx + dy * dy + dz * dz;
                    if (dist2 > rSq) {
                        continue;
                    }
                    pos.set(x, y, origin.getZ() + dz);
                    if (level.isOutsideBuildHeight(pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (!BlockImmutabilityUtil.canBlockBeDestroyed(level, pos)) {
                        continue;
                    }
                    list.add(pos.asLong(), dist2);
                }
            }
        }

        list.sortByDistanceAscending();
        return list;
    }

    private static int estimateInitialCapacity(double radius) {
        double estimate = 4.2 * radius * radius * radius;
        if (estimate < 16.0) {
            return 16;
        }
        if (estimate > 1_000_000.0) {
            return 1_000_000;
        }
        return (int) estimate;
    }
}

