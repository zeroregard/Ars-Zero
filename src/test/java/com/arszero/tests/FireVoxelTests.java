package com.arszero.tests;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.registry.ModEntities;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public class FireVoxelTests {
    public static void registerGameTests(RegisterGameTestsEvent event) {
        event.register(FireVoxelTests.class);
    }

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelIgnitesLog(GameTestHelper helper) {
        BlockPos logPos = new BlockPos(2, 0, 2);
        helper.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState());
        helper.setBlock(logPos.below(), Blocks.DIRT.defaultBlockState());
        helper.setBlock(logPos.above(), Blocks.AIR.defaultBlockState());

        ServerLevel level = helper.getLevel();
        BlockPos firePos = helper.absolutePos(logPos.above());
        BlockPos spawnPos = helper.absolutePos(logPos.above(3));

        FireVoxelEntity voxel = ModEntities.FIRE_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create FireVoxelEntity for test setup.");
            return;
        }

        voxel.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        voxel.setDeltaMovement(0.0D, -0.5D, 0.0D);
        voxel.setLifetime(200);
        level.addFreshEntity(voxel);

        AtomicBoolean voxelSeenBeforeImpact = new AtomicBoolean(voxel.isAlive());
        waitForImpact(helper, voxel, firePos, voxelSeenBeforeImpact, 200);
    }

    private static void waitForImpact(
        GameTestHelper helper,
        FireVoxelEntity voxel,
        BlockPos firePos,
        AtomicBoolean voxelSeenBeforeImpact,
        int ticksRemaining
    ) {
        if (ticksRemaining <= 0) {
            helper.fail("Fire voxel never collided with the log within the allotted time.");
            return;
        }

        helper.runAfterDelay(1, () -> {
            if (voxel.isAlive()) {
                voxelSeenBeforeImpact.set(true);
                waitForImpact(helper, voxel, firePos, voxelSeenBeforeImpact, ticksRemaining - 1);
                return;
            }

            if (!voxelSeenBeforeImpact.get()) {
                helper.fail("Fire voxel must exist before impact to validate collision behavior.");
                return;
            }

            BlockState state = helper.getLevel().getBlockState(firePos);
            if (!state.is(Blocks.FIRE)) {
                helper.fail("Fire block should be present above the log after the voxel collides.");
                return;
            }

            helper.succeed();
        });
    }
}

