package com.arszero.tests;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.WindVoxelEntity;
import com.github.ars_zero.registry.ModEntities;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public class WindVoxelWorldInteractionBehaviour {
    private static final BlockPos CENTER_RELATIVE = new BlockPos(2, 1, 2);
    private static final Vec3 DEFAULT_VEL = new Vec3(0.45D, 0.0D, 0.0D);
    private static final int COLLISION_TIMEOUT = 200;
    private static final int DEFAULT_LIFETIME = 200;
    private static final float DEFAULT_SIZE = 0.25f;
    
    public static void registerGameTests(RegisterGameTestsEvent event) {
        if (TestRegistrationFilter.shouldRegister(WindVoxelWorldInteractionBehaviour.class)) {
            event.register(WindVoxelWorldInteractionBehaviour.class);
        }
    }
    
    @GameTest(batch = "WindVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireInteractionCausesSmallExplosion(GameTestHelper helper) {
        BlockPos firePos = CENTER_RELATIVE;
        BlockPos fragilePos = CENTER_RELATIVE.offset(1, 0, 0);
        helper.setBlock(firePos, Blocks.FIRE.defaultBlockState());
        helper.setBlock(fragilePos, Blocks.GLASS.defaultBlockState());
        
        WindVoxelEntity wind = createWind(helper, DEFAULT_SIZE);
        if (wind == null) {
            return;
        }
        VoxelTestUtils.spawnVoxel(helper, wind, helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 0)), DEFAULT_VEL, DEFAULT_LIFETIME);
        AtomicBoolean seen = new AtomicBoolean(wind.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            wind,
            seen,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(2, () -> {
                BlockState fragile = helper.getBlockState(fragilePos);
                if (!fragile.isAir()) {
                    helper.fail("Fragile block should be destroyed by small explosion on fire interaction.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Wind voxel did not collide within timeout."),
            () -> helper.fail("Wind voxel must exist before impact.")
        );
    }
    
    @GameTest(batch = "WindVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterInteractionHasNoSideEffect(GameTestHelper helper) {
        BlockPos waterPos = CENTER_RELATIVE;
        helper.setBlock(waterPos, Blocks.WATER.defaultBlockState());
        
        WindVoxelEntity wind = createWind(helper, DEFAULT_SIZE);
        if (wind == null) {
            return;
        }
        VoxelTestUtils.spawnVoxel(helper, wind, helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 0)), DEFAULT_VEL, DEFAULT_LIFETIME);
        AtomicBoolean seen = new AtomicBoolean(wind.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            wind,
            seen,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(2, () -> {
                BlockState after = helper.getBlockState(waterPos);
                if (!after.is(Blocks.WATER)) {
                    helper.fail("Water block should remain unchanged after wind interaction.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Wind voxel did not collide within timeout."),
            () -> helper.fail("Wind voxel must exist before impact.")
        );
    }
    
    @GameTest(batch = "WindVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void windHitsStoneOnlyParticles(GameTestHelper helper) {
        BlockPos stonePos = CENTER_RELATIVE;
        helper.setBlock(stonePos, Blocks.STONE.defaultBlockState());
        
        WindVoxelEntity wind = createWind(helper, DEFAULT_SIZE);
        if (wind == null) {
            return;
        }
        VoxelTestUtils.spawnVoxel(helper, wind, helper.absolutePos(CENTER_RELATIVE.offset(-1, 0, 0)), DEFAULT_VEL, DEFAULT_LIFETIME);
        AtomicBoolean seen = new AtomicBoolean(wind.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            wind,
            seen,
            COLLISION_TIMEOUT,
            () -> helper.runAfterDelay(2, () -> {
                BlockState after = helper.getBlockState(stonePos);
                if (!after.is(Blocks.STONE)) {
                    helper.fail("Stone block should remain unchanged after wind voxel collision - only particles should spawn.");
                    return;
                }
                helper.succeed();
            }),
            () -> helper.fail("Wind voxel did not collide within timeout."),
            () -> helper.fail("Wind voxel must exist before impact.")
        );
    }
    
    @GameTest(batch = "WindVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void windPushesEntityAlongVelocity(GameTestHelper helper) {
        BlockPos spawn = CENTER_RELATIVE;
        ServerLevel level = helper.getLevel();
        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            helper.fail("Failed to create zombie for push test.");
            return;
        }
        Vec3 start = new Vec3(helper.absolutePos(spawn).getX() + 0.5D, helper.absolutePos(spawn).getY(), helper.absolutePos(spawn).getZ() + 0.5D);
        zombie.moveTo(start.x + 0.5D, start.y, start.z);
        level.addFreshEntity(zombie);
        
        WindVoxelEntity wind = createWind(helper, DEFAULT_SIZE);
        if (wind == null) {
            return;
        }
        VoxelTestUtils.spawnVoxel(helper, wind, helper.absolutePos(spawn.offset(-1, 0, 0)), DEFAULT_VEL, DEFAULT_LIFETIME);
        
        helper.runAfterDelay(10, () -> {
            Vec3 posAfter = zombie.position();
            if (posAfter.x <= start.x + 0.3D) {
                helper.fail("Zombie should be pushed forward by wind voxel impact.");
                return;
            }
            helper.succeed();
        });
    }
    
    @GameTest(batch = "WindVoxelWorldInteractionBehaviour", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void windPushesItemEntityAlongVelocity(GameTestHelper helper) {
        BlockPos spawn = CENTER_RELATIVE;
        ServerLevel level = helper.getLevel();
        helper.setBlock(spawn.below(), Blocks.STONE.defaultBlockState());
        
        Vec3 start = new Vec3(helper.absolutePos(spawn).getX() + 0.5D, helper.absolutePos(spawn).getY(), helper.absolutePos(spawn).getZ() + 0.5D);
        ItemEntity itemEntity = new ItemEntity(level, start.x, start.y, start.z, new ItemStack(Items.DIAMOND));
        itemEntity.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(itemEntity);
        
        WindVoxelEntity wind = createWind(helper, DEFAULT_SIZE);
        if (wind == null) {
            return;
        }
        VoxelTestUtils.spawnVoxel(helper, wind, helper.absolutePos(spawn.offset(-1, 0, 0)), DEFAULT_VEL, DEFAULT_LIFETIME);
        
        helper.runAfterDelay(10, () -> {
            if (!itemEntity.isAlive()) {
                helper.fail("Item entity should still be alive after wind voxel impact.");
                return;
            }
            Vec3 posAfter = itemEntity.position();
            Vec3 velocityAfter = itemEntity.getDeltaMovement();
            if (posAfter.x <= start.x + 0.3D && velocityAfter.lengthSqr() < 0.01D) {
                helper.fail("Item entity should be pushed forward by wind voxel impact.");
                return;
            }
            if (!itemEntity.hasPickUpDelay()) {
                helper.fail("Item entity should have pickup delay set after wind voxel impact.");
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
}


