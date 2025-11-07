package com.arszero.tests;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.registry.ModEntities;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@GameTestHolder(ArsZero.MOD_ID)
public class WaterVoxelTests {

    @GameTest(template = "arszero:water_voxel_test")
    public void waterVoxelCreatesAndEvaporatesWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos grassPos = helper.absolutePos(BlockPos.ZERO.offset(2, 0, 2));
        BlockPos waterPos = grassPos.above();
        BlockPos spawnPos = grassPos.above(2);

        WaterVoxelEntity voxel = ModEntities.WATER_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create WaterVoxelEntity for test setup.");
            return;
        }

        voxel.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        voxel.setDeltaMovement(Vec3.ZERO);
        voxel.setLifetime(200);
        level.addFreshEntity(voxel);

        AtomicBoolean voxelSeenBeforeImpact = new AtomicBoolean(voxel.isAlive());
        waitForImpact(helper, voxel, waterPos, voxelSeenBeforeImpact, 200);
    }

    private static void waitForImpact(
        GameTestHelper helper,
        WaterVoxelEntity voxel,
        BlockPos waterPos,
        AtomicBoolean voxelSeenBeforeImpact,
        int ticksRemaining
    ) {
        if (ticksRemaining <= 0) {
            helper.fail("Water voxel never collided with the grass block within the allotted time.");
            return;
        }

        helper.runAfterDelay(1, () -> {
            if (voxel.isAlive()) {
                voxelSeenBeforeImpact.set(true);
                waitForImpact(helper, voxel, waterPos, voxelSeenBeforeImpact, ticksRemaining - 1);
                return;
            }

            // Assert that the voxel existed prior to making contact with the grass block.
            if (!voxelSeenBeforeImpact.get()) {
                helper.fail("Water voxel must exist before impact to validate collision behavior.");
                return;
            }

            BlockState state = helper.getLevel().getBlockState(waterPos);

            // Assert that a water block appears directly above the grass after the collision.
            if (!state.is(Blocks.WATER)) {
                helper.fail("Water block should be present above the grass after the voxel collides.");
                return;
            }

            // Assert that the spawned water block has a level of exactly 1.
            if (!state.hasProperty(LiquidBlock.LEVEL) || state.getValue(LiquidBlock.LEVEL) != 1) {
                helper.fail("Water block above the grass should have level 1 after impact.");
                return;
            }

            waitForEvaporation(helper, waterPos, 200);
        });
    }

    private static void waitForEvaporation(GameTestHelper helper, BlockPos waterPos, int ticksRemaining) {
        if (ticksRemaining <= 0) {
            helper.fail("Water block did not evaporate within the expected timeframe.");
            return;
        }

        helper.runAfterDelay(1, () -> {
            BlockState state = helper.getLevel().getBlockState(waterPos);
            // Assert that the placed water block eventually evaporates and leaves air in its place.
            if (state.isAir()) {
                helper.succeed();
                return;
            }
            waitForEvaporation(helper, waterPos, ticksRemaining - 1);
        });
    }
}
