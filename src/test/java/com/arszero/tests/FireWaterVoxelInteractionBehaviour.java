package com.arszero.tests;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.registry.ModEntities;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public class FireWaterVoxelInteractionBehaviour {
    private static final BlockPos CENTER_RELATIVE = new BlockPos(2, 1, 2);
    private static final BlockPos FIRE_START_OFFSET = new BlockPos(-1, 0, 0);
    private static final Vec3 FIRE_COLLISION_VELOCITY = new Vec3(0.35D, 0.0D, 0.0D);
    private static final float DEFAULT_SIZE = 0.25f;
    private static final int DEFAULT_LIFETIME = 200;
    private static final int COLLISION_TIMEOUT = 200;
    private static final float FLOAT_TOLERANCE = 0.0001f;

    public static void registerGameTests(RegisterGameTestsEvent event) {
        if (TestRegistrationFilter.shouldRegister(FireWaterVoxelInteractionBehaviour.class)) {
            event.register(FireWaterVoxelInteractionBehaviour.class);
        }
    }

    @GameTest(batch = "FireWaterVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireAndWaterOfEqualSizeDestroyEachOther(GameTestHelper helper) {
        WaterVoxelEntity water = createWater(helper, DEFAULT_SIZE);
        if (water == null) {
            return;
        }
        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (fire == null) {
            return;
        }

        spawnWater(helper, water);
        spawnFire(helper, fire);

        AtomicBoolean fireSeen = new AtomicBoolean(fire.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            fire,
            fireSeen,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (water.isAlive()) {
                    helper.fail("Water voxel should be removed after colliding with equal-sized fire voxel.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Fire voxel never collided with the water voxel within the allotted time."),
            () -> helper.fail("Fire voxel must exist before impact to validate equal size interaction.")
        );
    }

    @GameTest(batch = "FireWaterVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void largerWaterVoxelSurvivesWithReducedSize(GameTestHelper helper) {
        float waterSize = DEFAULT_SIZE * 2.0f;
        WaterVoxelEntity water = createWater(helper, waterSize);
        if (water == null) {
            return;
        }
        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (fire == null) {
            return;
        }

        spawnWater(helper, water);
        spawnFire(helper, fire);

        float expectedSize = waterSize - DEFAULT_SIZE;
        AtomicBoolean fireSeen = new AtomicBoolean(fire.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            fire,
            fireSeen,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (!water.isAlive()) {
                    helper.fail("Water voxel should remain after overpowering the fire voxel.");
                    return;
                }
                float actualSize = water.getSize();
                if (Math.abs(actualSize - expectedSize) > FLOAT_TOLERANCE) {
                    helper.fail("Water voxel size should reduce to " + expectedSize + " but was " + actualSize + ".");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Fire voxel never reached the larger water voxel within the allotted time."),
            () -> helper.fail("Fire voxel must exist before impact to validate larger water interaction.")
        );
    }

    @GameTest(batch = "FireWaterVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void largerFireVoxelSurvivesWithReducedSize(GameTestHelper helper) {
        WaterVoxelEntity water = createWater(helper, DEFAULT_SIZE);
        if (water == null) {
            return;
        }
        float fireSize = DEFAULT_SIZE * 2.0f;
        FireVoxelEntity fire = createFire(helper, fireSize);
        if (fire == null) {
            return;
        }

        spawnWater(helper, water);
        spawnFire(helper, fire);

        float expectedSize = fireSize - DEFAULT_SIZE;
        AtomicBoolean waterSeen = new AtomicBoolean(water.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            water,
            waterSeen,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (!fire.isAlive()) {
                    helper.fail("Fire voxel should remain after overpowering the water voxel.");
                    return;
                }
                float actualSize = fire.getSize();
                if (Math.abs(actualSize - expectedSize) > FLOAT_TOLERANCE) {
                    helper.fail("Fire voxel size should reduce to " + expectedSize + " but was " + actualSize + ".");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Water voxel never reached the larger fire voxel within the allotted time."),
            () -> helper.fail("Water voxel must exist before impact to validate larger fire interaction.")
        );
    }

    @GameTest(batch = "FireWaterVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelExtinguishesBurningEntity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            helper.fail("Failed to create Zombie for entity extinguish test.");
            return;
        }
        BlockPos entityPos = helper.absolutePos(new BlockPos(2, 1, 2));
        zombie.moveTo(entityPos.getX() + 0.5D, entityPos.getY(), entityPos.getZ() + 0.5D, 0.0F, 0.0F);
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        zombie.igniteForSeconds(8);
        level.addFreshEntity(zombie);

        WaterVoxelEntity water = createWater(helper, DEFAULT_SIZE);
        if (water == null) {
            return;
        }

        BlockPos spawnPos = entityPos.above();
        VoxelTestUtils.spawnVoxel(helper, water, helper.absolutePos(spawnPos), new Vec3(0.0D, -0.3D, 0.0D), DEFAULT_LIFETIME);

        AtomicBoolean waterSeen = new AtomicBoolean(water.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            water,
            waterSeen,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (zombie.isOnFire()) {
                    helper.fail("Zombie should no longer be on fire after the water voxel collision.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Water voxel never collided with the burning entity within the allotted time."),
            () -> helper.fail("Water voxel must exist before impact to validate burning entity extinguish behaviour.")
        );
    }

    @GameTest(batch = "FireWaterVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelExtinguishesFire(GameTestHelper helper) {
        BlockPos baseRelative = new BlockPos(2, 0, 2);
        helper.setBlock(baseRelative, Blocks.NETHERRACK.defaultBlockState());
        BlockPos fireRelative = baseRelative.above();
        helper.setBlock(fireRelative, Blocks.FIRE.defaultBlockState());

        WaterVoxelEntity water = createWater(helper, DEFAULT_SIZE);
        if (water == null) {
            return;
        }

        BlockPos spawnPos = helper.absolutePos(fireRelative.above());
        VoxelTestUtils.spawnVoxel(helper, water, spawnPos, new Vec3(0.0D, -0.3D, 0.0D), DEFAULT_LIFETIME);

        AtomicBoolean waterSeen = new AtomicBoolean(water.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            water,
            waterSeen,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                BlockState extinguishedState = helper.getLevel().getBlockState(helper.absolutePos(fireRelative));
                if (!extinguishedState.is(Blocks.WATER)) {
                    helper.fail("Fire block should be replaced with water after the water voxel collision.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Water voxel never collided with the fire block within the allotted time."),
            () -> helper.fail("Water voxel must exist before impact to validate fire extinguish behaviour.")
        );
    }

    private static WaterVoxelEntity createWater(GameTestHelper helper, float size) {
        ServerLevel level = helper.getLevel();
        WaterVoxelEntity voxel = ModEntities.WATER_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create WaterVoxelEntity for fire-water interaction test.");
            return null;
        }
        voxel.setSize(size);
        voxel.refreshDimensions();
        voxel.setNoGravityCustom(true);
        voxel.setDeltaMovement(Vec3.ZERO);
        voxel.setLifetime(DEFAULT_LIFETIME);
        return voxel;
    }

    private static FireVoxelEntity createFire(GameTestHelper helper, float size) {
        ServerLevel level = helper.getLevel();
        FireVoxelEntity voxel = ModEntities.FIRE_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create FireVoxelEntity for fire-water interaction test.");
            return null;
        }
        voxel.setSize(size);
        voxel.refreshDimensions();
        voxel.setNoGravityCustom(true);
        voxel.setDeltaMovement(FIRE_COLLISION_VELOCITY);
        voxel.setLifetime(DEFAULT_LIFETIME);
        return voxel;
    }

    private static void spawnWater(GameTestHelper helper, WaterVoxelEntity voxel) {
        VoxelTestUtils.spawnVoxel(helper, voxel, helper.absolutePos(CENTER_RELATIVE), Vec3.ZERO, voxel.getLifetime());
    }

    private static void spawnFire(GameTestHelper helper, FireVoxelEntity voxel) {
        BlockPos spawnPos = helper.absolutePos(CENTER_RELATIVE.offset(FIRE_START_OFFSET));
        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, FIRE_COLLISION_VELOCITY, voxel.getLifetime());
    }
}
