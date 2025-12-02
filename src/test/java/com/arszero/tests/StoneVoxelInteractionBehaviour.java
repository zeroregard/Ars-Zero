package com.arszero.tests;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.StoneVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.common.entity.WindVoxelEntity;
import com.github.ars_zero.registry.ModEntities;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public class StoneVoxelInteractionBehaviour {
    private static final BlockPos CENTER_RELATIVE = new BlockPos(2, 1, 2);
    private static final int DEFAULT_LIFETIME = 200;
    private static final int COLLISION_TIMEOUT = 200;
    private static final float DEFAULT_SIZE = 0.25f;
    
    public static void registerGameTests(RegisterGameTestsEvent event) {
        if (TestRegistrationFilter.shouldRegister(StoneVoxelInteractionBehaviour.class)) {
            event.register(StoneVoxelInteractionBehaviour.class);
        }
    }
    
    @GameTest(batch = "StoneVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void stoneDestroysWindOnCollision(GameTestHelper helper) {
        WindVoxelEntity wind = createWind(helper, DEFAULT_SIZE);
        StoneVoxelEntity stone = createStone(helper, DEFAULT_SIZE);
        if (wind == null || stone == null) return;
        
        BlockPos left = helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 2));
        BlockPos right = helper.absolutePos(CENTER_RELATIVE.offset(1, 0, 2));
        
        VoxelTestUtils.spawnVoxel(helper, wind, left, new Vec3(0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        VoxelTestUtils.spawnVoxel(helper, stone, right, new Vec3(-0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        
        AtomicBoolean seenWind = new AtomicBoolean(wind.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            wind,
            seenWind,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (!stone.isAlive()) {
                    helper.fail("Stone voxel should continue after destroying wind voxel.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Wind voxel did not collide with stone within timeout."),
            () -> helper.fail("Wind voxel must exist before impact.")
        );
    }
    
    @GameTest(batch = "StoneVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void stoneExtinguishesFireOnCollision(GameTestHelper helper) {
        StoneVoxelEntity stone = createStone(helper, DEFAULT_SIZE);
        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (stone == null || fire == null) return;
        
        BlockPos left = helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 2));
        BlockPos right = helper.absolutePos(CENTER_RELATIVE.offset(1, 0, 2));
        
        VoxelTestUtils.spawnVoxel(helper, stone, left, new Vec3(0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        VoxelTestUtils.spawnVoxel(helper, fire, right, new Vec3(-0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        
        AtomicBoolean seenFire = new AtomicBoolean(fire.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            fire,
            seenFire,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (!stone.isAlive()) {
                    helper.fail("Stone voxel should continue after extinguishing fire voxel.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Fire voxel did not collide with stone within timeout."),
            () -> helper.fail("Fire voxel must exist before impact.")
        );
    }
    
    @GameTest(batch = "StoneVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void stoneBlocksWaterOnCollision(GameTestHelper helper) {
        StoneVoxelEntity stone = createStone(helper, DEFAULT_SIZE);
        WaterVoxelEntity water = createWater(helper, DEFAULT_SIZE);
        if (stone == null || water == null) return;
        
        BlockPos left = helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 2));
        BlockPos right = helper.absolutePos(CENTER_RELATIVE.offset(1, 0, 2));
        
        VoxelTestUtils.spawnVoxel(helper, stone, left, new Vec3(0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        VoxelTestUtils.spawnVoxel(helper, water, right, new Vec3(-0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        
        AtomicBoolean seenWater = new AtomicBoolean(water.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            water,
            seenWater,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (!stone.isAlive()) {
                    helper.fail("Stone voxel should continue after blocking water voxel.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Water voxel did not collide with stone within timeout."),
            () -> helper.fail("Water voxel must exist before impact.")
        );
    }
    
    @GameTest(batch = "StoneVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void twoStoneVoxelsBreakOnCollision(GameTestHelper helper) {
        StoneVoxelEntity stone1 = createStone(helper, DEFAULT_SIZE);
        StoneVoxelEntity stone2 = createStone(helper, DEFAULT_SIZE);
        if (stone1 == null || stone2 == null) return;
        
        BlockPos left = helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 2));
        BlockPos right = helper.absolutePos(CENTER_RELATIVE.offset(1, 0, 2));
        
        VoxelTestUtils.spawnVoxel(helper, stone1, left, new Vec3(0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        VoxelTestUtils.spawnVoxel(helper, stone2, right, new Vec3(-0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        
        AtomicBoolean seenStone1 = new AtomicBoolean(stone1.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            stone1,
            seenStone1,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (stone2.isAlive()) {
                    helper.fail("Both stone voxels should break on collision.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Stone voxels did not collide within timeout."),
            () -> helper.fail("Stone voxel must exist before impact.")
        );
    }
    
    @GameTest(batch = "StoneVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void twoWindVoxelsMergeOnCollision(GameTestHelper helper) {
        WindVoxelEntity wind1 = createWind(helper, DEFAULT_SIZE);
        WindVoxelEntity wind2 = createWind(helper, DEFAULT_SIZE);
        if (wind1 == null || wind2 == null) return;
        
        BlockPos left = helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 2));
        BlockPos right = helper.absolutePos(CENTER_RELATIVE.offset(1, 0, 2));
        
        VoxelTestUtils.spawnVoxel(helper, wind1, left, new Vec3(0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        VoxelTestUtils.spawnVoxel(helper, wind2, right, new Vec3(-0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        
        AtomicBoolean seenWind2 = new AtomicBoolean(wind2.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            wind2,
            seenWind2,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (!wind1.isAlive()) {
                    helper.fail("Wind voxel should merge and continue after collision.");
                    return;
                }
                float expectedSize = (float) Math.cbrt(DEFAULT_SIZE * DEFAULT_SIZE * DEFAULT_SIZE * 2);
                float actualSize = wind1.getSize();
                float tolerance = 0.01f;
                if (Math.abs(actualSize - expectedSize) > tolerance) {
                    helper.fail("Merged wind voxel size should be " + expectedSize + " but was " + actualSize + ".");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Wind voxels did not collide within timeout."),
            () -> helper.fail("Wind voxel must exist before impact.")
        );
    }
    
    private static StoneVoxelEntity createStone(GameTestHelper helper, float size) {
        ServerLevel level = helper.getLevel();
        StoneVoxelEntity stone = ModEntities.STONE_VOXEL_ENTITY.get().create(level);
        if (stone == null) {
            helper.fail("Failed to create StoneVoxelEntity.");
            return null;
        }
        stone.setSize(size);
        stone.refreshDimensions();
        return stone;
    }
    
    private static WindVoxelEntity createWind(GameTestHelper helper, float size) {
        ServerLevel level = helper.getLevel();
        WindVoxelEntity wind = ModEntities.WIND_VOXEL_ENTITY.get().create(level);
        if (wind == null) {
            helper.fail("Failed to create WindVoxelEntity.");
            return null;
        }
        wind.setSize(size);
        wind.refreshDimensions();
        return wind;
    }
    
    private static FireVoxelEntity createFire(GameTestHelper helper, float size) {
        ServerLevel level = helper.getLevel();
        FireVoxelEntity fire = ModEntities.FIRE_VOXEL_ENTITY.get().create(level);
        if (fire == null) {
            helper.fail("Failed to create FireVoxelEntity.");
            return null;
        }
        fire.setSize(size);
        fire.refreshDimensions();
        return fire;
    }
    
    private static WaterVoxelEntity createWater(GameTestHelper helper, float size) {
        ServerLevel level = helper.getLevel();
        WaterVoxelEntity water = ModEntities.WATER_VOXEL_ENTITY.get().create(level);
        if (water == null) {
            helper.fail("Failed to create WaterVoxelEntity.");
            return null;
        }
        water.setSize(size);
        water.refreshDimensions();
        return water;
    }
}



