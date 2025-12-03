package com.arszero.tests;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.IceVoxelEntity;
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
public class IceVoxelInteractionBehaviour {
    private static final BlockPos CENTER_RELATIVE = new BlockPos(2, 1, 2);
    private static final int DEFAULT_LIFETIME = 200;
    private static final int COLLISION_TIMEOUT = 200;
    private static final float DEFAULT_SIZE = 0.25f;
    
    public static void registerGameTests(RegisterGameTestsEvent event) {
        if (TestRegistrationFilter.shouldRegister(IceVoxelInteractionBehaviour.class)) {
            event.register(IceVoxelInteractionBehaviour.class);
        }
    }
    
    @GameTest(batch = "IceVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void iceDestroysWindOnCollision(GameTestHelper helper) {
        WindVoxelEntity wind = createWind(helper, DEFAULT_SIZE);
        IceVoxelEntity ice = createIce(helper, DEFAULT_SIZE);
        if (wind == null || ice == null) return;
        
        BlockPos left = helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 2));
        BlockPos right = helper.absolutePos(CENTER_RELATIVE.offset(1, 0, 2));
        
        VoxelTestUtils.spawnVoxel(helper, wind, left, new Vec3(0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        VoxelTestUtils.spawnVoxel(helper, ice, right, new Vec3(-0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        
        AtomicBoolean seenWind = new AtomicBoolean(wind.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            wind,
            seenWind,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (!ice.isAlive()) {
                    helper.fail("Ice voxel should continue after destroying wind voxel.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Wind voxel did not collide with ice within timeout."),
            () -> helper.fail("Wind voxel must exist before impact.")
        );
    }
    
    @GameTest(batch = "IceVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void iceExtinguishesFireOnCollision(GameTestHelper helper) {
        IceVoxelEntity ice = createIce(helper, DEFAULT_SIZE);
        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (ice == null || fire == null) return;
        
        BlockPos left = helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 2));
        BlockPos right = helper.absolutePos(CENTER_RELATIVE.offset(1, 0, 2));
        
        VoxelTestUtils.spawnVoxel(helper, ice, left, new Vec3(0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        VoxelTestUtils.spawnVoxel(helper, fire, right, new Vec3(-0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        
        AtomicBoolean seenFire = new AtomicBoolean(fire.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            fire,
            seenFire,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (!ice.isAlive()) {
                    helper.fail("Ice voxel should continue after extinguishing fire voxel.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Fire voxel did not collide with ice within timeout."),
            () -> helper.fail("Fire voxel must exist before impact.")
        );
    }
    
    @GameTest(batch = "IceVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void iceBlocksWaterOnCollision(GameTestHelper helper) {
        IceVoxelEntity ice = createIce(helper, DEFAULT_SIZE);
        WaterVoxelEntity water = createWater(helper, DEFAULT_SIZE);
        if (ice == null || water == null) return;
        
        BlockPos left = helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 2));
        BlockPos right = helper.absolutePos(CENTER_RELATIVE.offset(1, 0, 2));
        
        VoxelTestUtils.spawnVoxel(helper, ice, left, new Vec3(0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        VoxelTestUtils.spawnVoxel(helper, water, right, new Vec3(-0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        
        AtomicBoolean seenWater = new AtomicBoolean(water.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            water,
            seenWater,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (!ice.isAlive()) {
                    helper.fail("Ice voxel should continue after blocking water voxel.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Water voxel did not collide with ice within timeout."),
            () -> helper.fail("Water voxel must exist before impact.")
        );
    }
    
    @GameTest(batch = "IceVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void twoIceVoxelsBreakOnCollision(GameTestHelper helper) {
        IceVoxelEntity ice1 = createIce(helper, DEFAULT_SIZE);
        IceVoxelEntity ice2 = createIce(helper, DEFAULT_SIZE);
        if (ice1 == null || ice2 == null) return;
        
        BlockPos left = helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 2));
        BlockPos right = helper.absolutePos(CENTER_RELATIVE.offset(1, 0, 2));
        
        VoxelTestUtils.spawnVoxel(helper, ice1, left, new Vec3(0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        VoxelTestUtils.spawnVoxel(helper, ice2, right, new Vec3(-0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        
        AtomicBoolean seenIce1 = new AtomicBoolean(ice1.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            ice1,
            seenIce1,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (ice2.isAlive()) {
                    helper.fail("Both ice voxels should break on collision.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Ice voxels did not collide within timeout."),
            () -> helper.fail("Ice voxel must exist before impact.")
        );
    }
    
    @GameTest(batch = "IceVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void iceBreaksOnStoneCollision(GameTestHelper helper) {
        IceVoxelEntity ice = createIce(helper, DEFAULT_SIZE);
        StoneVoxelEntity stone = createStone(helper, DEFAULT_SIZE);
        if (ice == null || stone == null) return;
        
        BlockPos left = helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 2));
        BlockPos right = helper.absolutePos(CENTER_RELATIVE.offset(1, 0, 2));
        
        VoxelTestUtils.spawnVoxel(helper, ice, left, new Vec3(0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        VoxelTestUtils.spawnVoxel(helper, stone, right, new Vec3(-0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        
        AtomicBoolean seenIce = new AtomicBoolean(ice.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            ice,
            seenIce,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (!stone.isAlive()) {
                    helper.fail("Stone voxel should continue after ice breaks.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Ice voxel did not collide with stone within timeout."),
            () -> helper.fail("Ice voxel must exist before impact.")
        );
    }
    
    private static IceVoxelEntity createIce(GameTestHelper helper, float size) {
        ServerLevel level = helper.getLevel();
        IceVoxelEntity ice = ModEntities.ICE_VOXEL_ENTITY.get().create(level);
        if (ice == null) {
            helper.fail("Failed to create IceVoxelEntity.");
            return null;
        }
        ice.setSize(size);
        ice.refreshDimensions();
        return ice;
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
