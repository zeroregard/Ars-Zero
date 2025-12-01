package com.arszero.tests;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.common.entity.WindVoxelEntity;
import com.github.ars_zero.registry.ModEntities;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public class WindVoxelInteractionBehaviour {
    private static final BlockPos CENTER_RELATIVE = new BlockPos(2, 1, 2);
    private static final int DEFAULT_LIFETIME = 200;
    private static final int COLLISION_TIMEOUT = 200;
    private static final float DEFAULT_SIZE = 0.25f;
    
    public static void registerGameTests(RegisterGameTestsEvent event) {
        if (TestRegistrationFilter.shouldRegister(WindVoxelInteractionBehaviour.class)) {
            event.register(WindVoxelInteractionBehaviour.class);
        }
    }
    
    @GameTest(batch = "WindVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void windAndFireDiscardOnCollision(GameTestHelper helper) {
        helper.setBlock(CENTER_RELATIVE.below(), Blocks.STONE.defaultBlockState());
        
        WindVoxelEntity wind = createWind(helper, DEFAULT_SIZE);
        FireVoxelEntity fire = createFire(helper, DEFAULT_SIZE);
        if (wind == null || fire == null) return;
        
        BlockPos left = helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 2));
        BlockPos right = helper.absolutePos(CENTER_RELATIVE.offset(1, 0, 2));
        
        VoxelTestUtils.spawnVoxel(helper, wind, left, new Vec3(0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        VoxelTestUtils.spawnVoxel(helper, fire, right, new Vec3(-0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        
        AtomicBoolean seenWind = new AtomicBoolean(wind.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            wind,
            seenWind,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(1, () -> {
                if (fire.isAlive()) {
                    helper.fail("Fire voxel should also be discarded after wind-fire interaction.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Wind voxel did not collide within timeout."),
            () -> helper.fail("Wind voxel must exist before impact.")
        );
    }
    
    @GameTest(batch = "WindVoxelInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void windAndWaterContinueOnCollision(GameTestHelper helper) {
        helper.setBlock(CENTER_RELATIVE.below(), Blocks.STONE.defaultBlockState());
        
        WindVoxelEntity wind = createWind(helper, DEFAULT_SIZE);
        WaterVoxelEntity water = createWater(helper, DEFAULT_SIZE);
        if (wind == null || water == null) return;
        
        BlockPos left = helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 2));
        BlockPos right = helper.absolutePos(CENTER_RELATIVE.offset(1, 0, 2));
        
        VoxelTestUtils.spawnVoxel(helper, wind, left, new Vec3(0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        VoxelTestUtils.spawnVoxel(helper, water, right, new Vec3(-0.2D, 0.0D, 0.0D), DEFAULT_LIFETIME);
        
        helper.runAfterDelay(20, () -> {
            if (!wind.isAlive() || !water.isAlive()) {
                helper.fail("Both wind and water voxels should continue after interaction.");
                return;
            }
            helper.succeed();
        });
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


