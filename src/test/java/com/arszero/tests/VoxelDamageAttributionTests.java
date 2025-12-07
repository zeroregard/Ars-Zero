package com.arszero.tests;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.StoneVoxelEntity;
import com.github.ars_zero.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public class VoxelDamageAttributionTests {
    private static final BlockPos CENTER_RELATIVE = new BlockPos(2, 1, 2);
    private static final int DEFAULT_LIFETIME = 200;
    private static final int TEST_TIMEOUT = 200;
    private static final float DEFAULT_SIZE = 0.25f;
    
    public static void registerGameTests(RegisterGameTestsEvent event) {
        if (TestRegistrationFilter.shouldRegister(VoxelDamageAttributionTests.class)) {
            event.register(VoxelDamageAttributionTests.class);
        }
    }
    
    @GameTest(batch = "VoxelDamageAttribution", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void stoneVoxelDamageAttributionToPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (player == null) {
            helper.fail("Failed to create mock player.");
            return;
        }
        
        BlockPos playerPos = helper.absolutePos(CENTER_RELATIVE.offset(0, 0, -2));
        player.setPos(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
        level.addFreshEntity(player);
        
        IronGolem golem = new IronGolem(net.minecraft.world.entity.EntityType.IRON_GOLEM, level);
        BlockPos golemPos = helper.absolutePos(CENTER_RELATIVE);
        golem.setPos(golemPos.getX() + 0.5, golemPos.getY(), golemPos.getZ() + 0.5);
        level.addFreshEntity(golem);
        
        helper.runAfterDelay(5, () -> {
            StoneVoxelEntity stone = ModEntities.STONE_VOXEL_ENTITY.get().create(level);
            if (stone == null) {
                helper.fail("Failed to create StoneVoxelEntity.");
                return;
            }
            stone.setSize(DEFAULT_SIZE);
            stone.refreshDimensions();
            stone.setCaster(player);
            
            BlockPos stoneSpawnPos = helper.absolutePos(CENTER_RELATIVE.offset(0, 0, -1));
            Vec3 direction = new Vec3(golemPos.getX() - stoneSpawnPos.getX(), 0, golemPos.getZ() - stoneSpawnPos.getZ()).normalize();
            Vec3 velocity = direction.scale(0.5);
            
            VoxelTestUtils.spawnVoxel(helper, stone, stoneSpawnPos, velocity, DEFAULT_LIFETIME);
            
            checkGolemRetaliation(helper, golem, player, 0, TEST_TIMEOUT);
        });
    }
    
    @GameTest(batch = "VoxelDamageAttribution", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void fireVoxelIgnitionAttributionToPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (player == null) {
            helper.fail("Failed to create mock player.");
            return;
        }
        
        BlockPos playerPos = helper.absolutePos(CENTER_RELATIVE.offset(0, 0, -2));
        player.setPos(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
        level.addFreshEntity(player);
        
        IronGolem golem = new IronGolem(net.minecraft.world.entity.EntityType.IRON_GOLEM, level);
        BlockPos golemPos = helper.absolutePos(CENTER_RELATIVE);
        golem.setPos(golemPos.getX() + 0.5, golemPos.getY(), golemPos.getZ() + 0.5);
        level.addFreshEntity(golem);
        
        helper.runAfterDelay(5, () -> {
            com.github.ars_zero.common.entity.FireVoxelEntity fire = ModEntities.FIRE_VOXEL_ENTITY.get().create(level);
            if (fire == null) {
                helper.fail("Failed to create FireVoxelEntity.");
                return;
            }
            fire.setSize(DEFAULT_SIZE);
            fire.refreshDimensions();
            fire.setCaster(player);
            
            BlockPos fireSpawnPos = helper.absolutePos(CENTER_RELATIVE.offset(0, 0, -1));
            Vec3 direction = new Vec3(golemPos.getX() - fireSpawnPos.getX(), 0, golemPos.getZ() - fireSpawnPos.getZ()).normalize();
            Vec3 velocity = direction.scale(0.5);
            
            VoxelTestUtils.spawnVoxel(helper, fire, fireSpawnPos, velocity, DEFAULT_LIFETIME);
            
            checkGolemRetaliation(helper, golem, player, 0, TEST_TIMEOUT);
        });
    }
    
    private static void checkGolemRetaliation(GameTestHelper helper, IronGolem golem, Player player, int ticksElapsed, int timeout) {
        if (ticksElapsed >= timeout) {
            helper.fail("Iron golem did not retaliate within timeout. Last hurt by: " + 
                (golem.getLastHurtByMob() != null ? golem.getLastHurtByMob().getName().getString() : "null") +
                ", Target: " + (golem.getTarget() != null ? golem.getTarget().getName().getString() : "null"));
            return;
        }
        
        helper.runAfterDelay(1, () -> {
            LivingEntity target = golem.getTarget();
            if (target == player) {
                helper.succeed();
                return;
            }
            
            if (golem.getLastHurtByMob() == player) {
                helper.succeed();
                return;
            }
            
            if (!golem.isAlive()) {
                helper.fail("Iron golem died before retaliating.");
                return;
            }
            
            checkGolemRetaliation(helper, golem, player, ticksElapsed + 1, timeout);
        });
    }
}

