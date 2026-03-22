package com.github.ars_zero.common.world.placement;

import com.github.ars_zero.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;

/**
 * Placement filter that rejects a position if any blight archwood log already exists
 * within a horizontal and vertical radius. Used for the huge dead archwood tree so
 * it does not place on top of another tree.
 */
public class NoBlightLogNearbyFilter extends PlacementFilter {

    public static final NoBlightLogNearbyFilter INSTANCE = new NoBlightLogNearbyFilter();

    /** Horizontal radius to scan (blocks from center). */
    private static final int RADIUS_H = 14;
    /** Vertical radius (blocks up and down from placement). */
    private static final int RADIUS_V = 32;

    private NoBlightLogNearbyFilter() {}

    public static final MapCodec<NoBlightLogNearbyFilter> CODEC = MapCodec.unit(INSTANCE);

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        var level = context.getLevel();
        var logBlock = ModBlocks.BLIGHT_ARCHWOOD_LOG.get();
        int minY = Math.max(level.getMinBuildHeight(), pos.getY() - RADIUS_V);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, pos.getY() + RADIUS_V);
        for (int y = minY; y <= maxY; y++) {
            for (int dx = -RADIUS_H; dx <= RADIUS_H; dx++) {
                for (int dz = -RADIUS_H; dz <= RADIUS_H; dz++) {
                    if (dx == 0 && dz == 0 && y == pos.getY()) continue;
                    BlockPos check = pos.offset(dx, y - pos.getY(), dz);
                    if (level.getBlockState(check).is(logBlock)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public net.minecraft.world.level.levelgen.placement.PlacementModifierType<?> type() {
        return com.github.ars_zero.registry.ModWorldgen.NO_BLIGHT_LOG_NEARBY_FILTER.get();
    }
}
