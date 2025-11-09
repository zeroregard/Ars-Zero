package com.arszero.tests;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.registry.ModEntities;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public class WaterVoxelTests {
    public static void registerGameTests(RegisterGameTestsEvent event) {
        event.register(WaterVoxelTests.class);
    }

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelCreatesAndEvaporatesWater(GameTestHelper helper) {
        BlockPos relativeGrassPos = new BlockPos(2, 0, 2);
        helper.setBlock(relativeGrassPos, Blocks.GRASS_BLOCK.defaultBlockState());
        helper.setBlock(relativeGrassPos.below(), Blocks.DIRT.defaultBlockState());
        helper.setBlock(relativeGrassPos.above(), Blocks.AIR.defaultBlockState());

        ServerLevel level = helper.getLevel();
        BlockPos waterPos = helper.absolutePos(relativeGrassPos.above());
        BlockPos spawnPos = helper.absolutePos(relativeGrassPos.above(2));

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

            if (!voxelSeenBeforeImpact.get()) {
                helper.fail("Water voxel must exist before impact to validate collision behavior.");
                return;
            }

            BlockState state = helper.getLevel().getBlockState(waterPos);

            if (!state.is(Blocks.WATER)) {
                helper.fail("Water block should be present above the grass after the voxel collides.");
                return;
            }

            if (!state.hasProperty(LiquidBlock.LEVEL) || state.getValue(LiquidBlock.LEVEL) != 6) {
                helper.fail("Water block above the grass should have level 6 after impact.");
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
            if (state.isAir()) {
                helper.succeed();
                return;
            }
            waitForEvaporation(helper, waterPos, ticksRemaining - 1);
        });
    }
}
