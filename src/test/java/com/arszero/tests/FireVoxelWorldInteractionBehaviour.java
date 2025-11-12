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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public class FireVoxelWorldInteractionBehaviour {
    private static final BlockPos CENTER_RELATIVE = new BlockPos(2, 1, 2);
    private static final BlockPos FIRE_START_OFFSET = new BlockPos(-1, 0, 0);
    private static final Vec3 FIRE_COLLISION_VELOCITY = new Vec3(0.35D, 0.0D, 0.0D);
    private static final float DEFAULT_SIZE = 0.25f;
    private static final int DEFAULT_LIFETIME = 200;
    private static final int COLLISION_TIMEOUT = 200;
    private static final float FLOAT_TOLERANCE = 0.0001f;

    public static void registerGameTests(RegisterGameTestsEvent event) {
        if (TestRegistrationFilter.shouldRegister(FireVoxelWorldInteractionBehaviour.class)) {
            event.register(FireVoxelWorldInteractionBehaviour.class);
        }
    }

    @GameTest(batch = "FireVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelIgnitesCampfire(GameTestHelper helper) {
        BlockPos campfirePos = helper.absolutePos(CENTER_RELATIVE);
        helper.setBlock(CENTER_RELATIVE, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false));

        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (fire == null) {
            return;
        }

        spawnFire(helper, fire);
        AtomicBoolean fireSeen = new AtomicBoolean(fire.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            fire,
            fireSeen,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                BlockState currentState = helper.getLevel().getBlockState(campfirePos);
                if (!currentState.is(Blocks.CAMPFIRE)) {
                    helper.fail("Campfire block should remain after fire voxel collision.");
                    return;
                }
                if (!currentState.getValue(BlockStateProperties.LIT)) {
                    helper.fail("Campfire block should be lit after fire voxel collision.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Fire voxel never collided with the campfire within the allotted time."),
            () -> helper.fail("Fire voxel must exist before impact to validate campfire ignition.")
        );
    }

    @GameTest(batch = "FireVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelActivatesNetherPortal(GameTestHelper helper) {
        BlockPos frameOrigin = new BlockPos(1, 0, 2);
        for (int x = 0; x < 4; x++) {
            helper.setBlock(frameOrigin.offset(x, 0, 0), Blocks.OBSIDIAN.defaultBlockState());
            helper.setBlock(frameOrigin.offset(x, 4, 0), Blocks.OBSIDIAN.defaultBlockState());
        }
        for (int y = 1; y < 4; y++) {
            helper.setBlock(frameOrigin.offset(0, y, 0), Blocks.OBSIDIAN.defaultBlockState());
            helper.setBlock(frameOrigin.offset(3, y, 0), Blocks.OBSIDIAN.defaultBlockState());
        }

        BlockPos interiorBase = new BlockPos(2, 1, 2);
        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (fire == null) {
            return;
        }

        BlockPos spawnPos = helper.absolutePos(interiorBase.above(2));
        VoxelTestUtils.spawnVoxel(helper, fire, spawnPos, new Vec3(0.0D, -0.4D, 0.0D), DEFAULT_LIFETIME);

        AtomicBoolean fireSeen = new AtomicBoolean(fire.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            fire,
            fireSeen,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(2, () -> {
                ServerLevel level = helper.getLevel();
                for (int x = 0; x < 2; x++) {
                    for (int y = 0; y < 3; y++) {
                        BlockPos portalPos = helper.absolutePos(new BlockPos(interiorBase.getX() + x, interiorBase.getY() + y, interiorBase.getZ()));
                        BlockState state = level.getBlockState(portalPos);
                        if (!state.is(Blocks.NETHER_PORTAL)) {
                            helper.fail("Interior of the portal frame should form Nether portal blocks.");
                            return;
                        }
                    }
                }
                helper.succeed();
            }),
            () -> helper.fail("Fire voxel never collided with the portal frame within the allotted time."),
            () -> helper.fail("Fire voxel must exist before impact to validate portal activation.")
        );
    }

    @GameTest(batch = "FireVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelIgnitesTnt(GameTestHelper helper) {
        BlockPos tntPos = helper.absolutePos(CENTER_RELATIVE);
        helper.setBlock(CENTER_RELATIVE, Blocks.TNT.defaultBlockState());

        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (fire == null) {
            return;
        }

        spawnFire(helper, fire);
        AtomicBoolean fireSeen = new AtomicBoolean(fire.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            fire,
            fireSeen,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                ServerLevel level = helper.getLevel();
                if (!level.getBlockState(tntPos).isAir()) {
                    helper.fail("TNT block should be consumed after ignition by the fire voxel.");
                    return;
                }
                AABB searchArea = new AABB(tntPos).inflate(1.5D);
                if (level.getEntitiesOfClass(PrimedTnt.class, searchArea).isEmpty()) {
                    helper.fail("Primed TNT entity should exist after the fire voxel ignition.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Fire voxel never collided with the TNT block within the allotted time."),
            () -> helper.fail("Fire voxel must exist before impact to validate TNT ignition.")
        );
    }

    @GameTest(batch = "FireVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelMeltsIce(GameTestHelper helper) {
        BlockPos icePos = helper.absolutePos(CENTER_RELATIVE);
        helper.setBlock(CENTER_RELATIVE, Blocks.ICE.defaultBlockState());

        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (fire == null) {
            return;
        }

        spawnFire(helper, fire);
        AtomicBoolean fireSeen = new AtomicBoolean(fire.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            fire,
            fireSeen,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                BlockState state = helper.getLevel().getBlockState(icePos);
                if (!state.is(Blocks.WATER)) {
                    helper.fail("Ice block should become water after the fire voxel collision.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Fire voxel never collided with the ice block within the allotted time."),
            () -> helper.fail("Fire voxel must exist before impact to validate ice melting.")
        );
    }

    @GameTest(batch = "FireVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelEvaporatesSnow(GameTestHelper helper) {
        BlockPos snowPos = helper.absolutePos(CENTER_RELATIVE);
        helper.setBlock(CENTER_RELATIVE, Blocks.SNOW.defaultBlockState());

        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (fire == null) {
            return;
        }

        spawnFire(helper, fire);
        AtomicBoolean fireSeen = new AtomicBoolean(fire.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            fire,
            fireSeen,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                BlockState state = helper.getLevel().getBlockState(snowPos);
                if (!state.isAir()) {
                    helper.fail("Snow layer should evaporate after the fire voxel collision.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Fire voxel never collided with the snow layer within the allotted time."),
            () -> helper.fail("Fire voxel must exist before impact to validate snow evaporation.")
        );
    }

    @GameTest(batch = "FireVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelRainShrinkAtZeroFirePower(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (fire == null) {
            return;
        }

        forceRain(level);
        fire.setNoGravityCustom(true);
        fire.setDeltaMovement(Vec3.ZERO);
        fire.setCasterFirePower(0.0f);

        float initialSize = fire.getSize();
        VoxelTestUtils.spawnVoxel(helper, fire, helper.absolutePos(CENTER_RELATIVE), Vec3.ZERO, DEFAULT_LIFETIME);

        helper.runAfterDelay(25, () -> {
            float expectedSize = initialSize * 0.95f;
            float actualSize = fire.getSize();
            if (Math.abs(actualSize - expectedSize) > FLOAT_TOLERANCE) {
                helper.fail("Fire voxel should shrink by 5% in rain at fire power 0. Expected " + expectedSize + " but was " + actualSize + ".");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(batch = "FireVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelRainShrinkAtOneFirePower(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (fire == null) {
            return;
        }

        forceRain(level);
        fire.setNoGravityCustom(true);
        fire.setDeltaMovement(Vec3.ZERO);
        fire.setCasterFirePower(1.0f);

        float initialSize = fire.getSize();
        VoxelTestUtils.spawnVoxel(helper, fire, helper.absolutePos(CENTER_RELATIVE), Vec3.ZERO, DEFAULT_LIFETIME);

        helper.runAfterDelay(25, () -> {
            float expectedSize = initialSize * 0.975f;
            float actualSize = fire.getSize();
            if (Math.abs(actualSize - expectedSize) > FLOAT_TOLERANCE) {
                helper.fail("Fire voxel should shrink by 2.5% in rain at fire power 1. Expected " + expectedSize + " but was " + actualSize + ".");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(batch = "FireVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelRainNoShrinkAtHighFirePower(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (fire == null) {
            return;
        }

        forceRain(level);
        fire.setNoGravityCustom(true);
        fire.setDeltaMovement(Vec3.ZERO);
        fire.setCasterFirePower(3.0f);

        float initialSize = fire.getSize();
        VoxelTestUtils.spawnVoxel(helper, fire, helper.absolutePos(CENTER_RELATIVE), Vec3.ZERO, DEFAULT_LIFETIME);

        helper.runAfterDelay(25, () -> {
            float actualSize = fire.getSize();
            if (Math.abs(actualSize - initialSize) > FLOAT_TOLERANCE) {
                helper.fail("Fire voxel should not shrink in rain at fire power 2 or greater. Expected " + initialSize + " but was " + actualSize + ".");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(batch = "FireVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelUnderwaterShrinkAtZeroFirePower(GameTestHelper helper) {
        BlockPos waterPos = helper.absolutePos(CENTER_RELATIVE);
        helper.setBlock(CENTER_RELATIVE, Blocks.WATER.defaultBlockState());

        ServerLevel level = helper.getLevel();
        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (fire == null) {
            return;
        }

        fire.setNoGravityCustom(true);
        fire.setDeltaMovement(Vec3.ZERO);
        fire.setCasterFirePower(0.0f);

        float initialSize = fire.getSize();
        VoxelTestUtils.spawnVoxel(helper, fire, waterPos, Vec3.ZERO, DEFAULT_LIFETIME);

        helper.runAfterDelay(25, () -> {
            float expectedSize = initialSize * 0.5f;
            float actualSize = fire.getSize();
            if (Math.abs(actualSize - expectedSize) > FLOAT_TOLERANCE) {
                helper.fail("Fire voxel should shrink by 50% when submerged at fire power 0. Expected " + expectedSize + " but was " + actualSize + ".");
                return;
            }
            if (!level.getBlockState(waterPos).isAir()) {
                helper.fail("Water block should evaporate after the fire voxel submersion.");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(batch = "FireVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelUnderwaterShrinkAtOneFirePower(GameTestHelper helper) {
        BlockPos waterPos = helper.absolutePos(CENTER_RELATIVE);
        helper.setBlock(CENTER_RELATIVE, Blocks.WATER.defaultBlockState());

        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (fire == null) {
            return;
        }

        fire.setNoGravityCustom(true);
        fire.setDeltaMovement(Vec3.ZERO);
        fire.setCasterFirePower(1.0f);

        float initialSize = fire.getSize();
        VoxelTestUtils.spawnVoxel(helper, fire, waterPos, Vec3.ZERO, DEFAULT_LIFETIME);

        helper.runAfterDelay(25, () -> {
            float expectedSize = initialSize * 0.75f;
            float actualSize = fire.getSize();
            if (Math.abs(actualSize - expectedSize) > FLOAT_TOLERANCE) {
                helper.fail("Fire voxel should shrink by 25% when submerged at fire power 1. Expected " + expectedSize + " but was " + actualSize + ".");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(batch = "FireVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelUnderwaterNoShrinkAtHighFirePower(GameTestHelper helper) {
        BlockPos waterPos = helper.absolutePos(CENTER_RELATIVE);
        helper.setBlock(CENTER_RELATIVE, Blocks.WATER.defaultBlockState());

        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (fire == null) {
            return;
        }

        fire.setNoGravityCustom(true);
        fire.setDeltaMovement(Vec3.ZERO);
        fire.setCasterFirePower(3.0f);

        float initialSize = fire.getSize();
        VoxelTestUtils.spawnVoxel(helper, fire, waterPos, Vec3.ZERO, DEFAULT_LIFETIME);

        helper.runAfterDelay(25, () -> {
            float actualSize = fire.getSize();
            if (Math.abs(actualSize - initialSize) > FLOAT_TOLERANCE) {
                helper.fail("Fire voxel should not shrink when submerged at fire power 2 or greater.");
                return;
            }
            helper.succeed();
        });
    }

    private static FireVoxelEntity createFire(GameTestHelper helper, float size) {
        ServerLevel level = helper.getLevel();
        FireVoxelEntity voxel = ModEntities.FIRE_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create FireVoxelEntity for fire-world interaction test.");
            return null;
        }
        voxel.setSize(size);
        voxel.refreshDimensions();
        voxel.setNoGravityCustom(true);
        voxel.setDeltaMovement(FIRE_COLLISION_VELOCITY);
        voxel.setLifetime(DEFAULT_LIFETIME);
        return voxel;
    }

    private static void spawnFire(GameTestHelper helper, FireVoxelEntity voxel) {
        BlockPos spawnPos = helper.absolutePos(CENTER_RELATIVE.offset(FIRE_START_OFFSET));
        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, FIRE_COLLISION_VELOCITY, voxel.getLifetime());
    }

    private static void forceRain(ServerLevel level) {
        level.setWeatherParameters(0, 6000, true, false);
    }
}

