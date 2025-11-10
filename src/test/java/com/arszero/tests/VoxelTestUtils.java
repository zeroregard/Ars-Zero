package com.arszero.tests;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class VoxelTestUtils {
    private VoxelTestUtils() {
    }

    public static void prepareColumn(
        GameTestHelper helper,
        BlockPos relativePos,
        BlockState centerBlock,
        BlockState belowBlock,
        BlockState aboveBlock
    ) {
        helper.setBlock(relativePos, centerBlock);
        helper.setBlock(relativePos.below(), belowBlock);
        helper.setBlock(relativePos.above(), aboveBlock);
    }

    public static <T extends BaseVoxelEntity> T spawnVoxel(
        GameTestHelper helper,
        T voxel,
        BlockPos spawnPos,
        Vec3 velocity,
        int lifetime
    ) {
        voxel.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        voxel.setDeltaMovement(velocity);
        voxel.setLifetime(lifetime);
        helper.getLevel().addFreshEntity(voxel);
        return voxel;
    }

    public static void awaitVoxelRemoval(
        GameTestHelper helper,
        BaseVoxelEntity voxel,
        AtomicBoolean seenBeforeRemoval,
        int ticksRemaining,
        Runnable onRemoved,
        Runnable onTimeout,
        Runnable onNotSeen
    ) {
        if (ticksRemaining <= 0) {
            onTimeout.run();
            return;
        }

        helper.runAfterDelay(1, () -> {
            if (voxel.isAlive()) {
                seenBeforeRemoval.set(true);
                awaitVoxelRemoval(helper, voxel, seenBeforeRemoval, ticksRemaining - 1, onRemoved, onTimeout, onNotSeen);
                return;
            }

            if (!seenBeforeRemoval.get()) {
                onNotSeen.run();
                return;
            }

            onRemoved.run();
        });
    }
}




