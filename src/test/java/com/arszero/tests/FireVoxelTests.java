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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public class FireVoxelTests {
    public static void registerGameTests(RegisterGameTestsEvent event) {
        if (TestRegistrationFilter.shouldRegister(FireVoxelTests.class)) {
            event.register(FireVoxelTests.class);
        }
    }

    @GameTest(batch = "FireVoxelTests", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelIgnitesLog(GameTestHelper helper) {
        BlockPos logPos = new BlockPos(2, 0, 2);
        VoxelTestUtils.prepareColumn(
            helper,
            logPos,
            Blocks.OAK_LOG.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(),
            Blocks.AIR.defaultBlockState()
        );

        ServerLevel level = helper.getLevel();
        BlockPos firePos = helper.absolutePos(logPos.above());
        BlockPos spawnPos = helper.absolutePos(logPos.above(3));

        FireVoxelEntity voxel = ModEntities.FIRE_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create FireVoxelEntity for test setup.");
            return;
        }

        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, new Vec3(0.0D, -0.5D, 0.0D), 200);

        AtomicBoolean voxelSeenBeforeImpact = new AtomicBoolean(voxel.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            voxel,
            voxelSeenBeforeImpact,
            200,
            () -> {
                BlockState state = helper.getLevel().getBlockState(firePos);
                if (!state.is(Blocks.FIRE)) {
                    helper.fail("Fire block should be present above the log after the voxel collides.");
                    return;
                }
                helper.succeed();
            },
            () -> helper.fail("Fire voxel never collided with the log within the allotted time."),
            () -> helper.fail("Fire voxel must exist before impact to validate collision behavior.")
        );
    }
}

