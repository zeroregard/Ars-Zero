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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;

@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public class WaterVoxelTests {
    public static void registerGameTests(RegisterGameTestsEvent event) {
        event.register(WaterVoxelTests.class);
    }

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelCreatesAndEvaporatesWater(GameTestHelper helper) {
        BlockPos relativeGrassPos = new BlockPos(2, 0, 2);
        VoxelTestUtils.prepareColumn(
            helper,
            relativeGrassPos,
            Blocks.GRASS_BLOCK.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(),
            Blocks.AIR.defaultBlockState()
        );

        ServerLevel level = helper.getLevel();
        BlockPos waterPos = helper.absolutePos(relativeGrassPos.above());
        BlockPos spawnPos = helper.absolutePos(relativeGrassPos.above(2));

        WaterVoxelEntity voxel = ModEntities.WATER_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create WaterVoxelEntity for test setup.");
            return;
        }

        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, Vec3.ZERO, 200);

        AtomicBoolean voxelSeenBeforeImpact = new AtomicBoolean(voxel.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            voxel,
            voxelSeenBeforeImpact,
            200,
            () -> {
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
            },
            () -> helper.fail("Water voxel never collided with the grass block within the allotted time."),
            () -> helper.fail("Water voxel must exist before impact to validate collision behavior.")
        );
    }

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelHydratesFarmland(GameTestHelper helper) {
        BlockPos relativeFarmlandPos = new BlockPos(2, 0, 2);
        VoxelTestUtils.prepareColumn(
            helper,
            relativeFarmlandPos,
            Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0),
            Blocks.DIRT.defaultBlockState(),
            Blocks.AIR.defaultBlockState()
        );

        ServerLevel level = helper.getLevel();
        BlockPos farmlandPos = helper.absolutePos(relativeFarmlandPos);
        BlockPos spawnPos = helper.absolutePos(relativeFarmlandPos.above());

        WaterVoxelEntity voxel = ModEntities.WATER_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create WaterVoxelEntity for test setup.");
            return;
        }

        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, Vec3.ZERO, 200);
        voxel.setNoGravityCustom(true);

        helper.runAfterDelay(25, () -> {
            BlockState farmlandState = helper.getLevel().getBlockState(farmlandPos);
            if (!farmlandState.is(Blocks.FARMLAND)) {
                helper.fail("Farmland block should remain farmland after hydration.");
                return;
            }
            if (!farmlandState.hasProperty(FarmBlock.MOISTURE)) {
                helper.fail("Farmland block must expose a moisture property.");
                return;
            }
            if (farmlandState.getValue(FarmBlock.MOISTURE) != FarmBlock.MAX_MOISTURE) {
                helper.fail("Farmland should be fully hydrated 20 ticks after the voxel appears.");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelFillsBucket(GameTestHelper helper) {
        BlockPos spawnRelativePos = new BlockPos(2, 1, 2);

        ServerLevel level = helper.getLevel();
        BlockPos spawnPos = helper.absolutePos(spawnRelativePos);

        WaterVoxelEntity voxel = ModEntities.WATER_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create WaterVoxelEntity for bucket test.");
            return;
        }

        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, Vec3.ZERO, 200);

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));

        InteractionResult result = voxel.interact(player, InteractionHand.MAIN_HAND);
        if (!result.consumesAction()) {
            helper.fail("Water voxel interaction with bucket should consume the action.");
            return;
        }

        helper.runAfterDelay(1, () -> {
            ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!held.is(Items.WATER_BUCKET)) {
                helper.fail("Player should hold a water bucket after interacting with the water voxel.");
                return;
            }
            if (voxel.isAlive()) {
                helper.fail("Water voxel should be removed after filling a bucket.");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelFillsPotionsAndShrinks(GameTestHelper helper) {
        BlockPos spawnRelativePos = new BlockPos(2, 1, 2);

        ServerLevel level = helper.getLevel();
        BlockPos spawnPos = helper.absolutePos(spawnRelativePos);

        WaterVoxelEntity voxel = ModEntities.WATER_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create WaterVoxelEntity for potion test.");
            return;
        }

        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, Vec3.ZERO, 200);
        float initialSize = voxel.getSize();

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));

        InteractionResult firstResult = voxel.interact(player, InteractionHand.MAIN_HAND);
        if (!firstResult.consumesAction()) {
            helper.fail("First potion interaction should consume the action.");
            return;
        }

        ItemStack firstHeld = player.getItemInHand(InteractionHand.MAIN_HAND);
        PotionContents firstContents = firstHeld.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (!firstHeld.is(Items.POTION) || !firstContents.is(Potions.WATER)) {
            helper.fail("First interaction should yield a water potion.");
            return;
        }
        if (!(voxel.getSize() < initialSize)) {
            helper.fail("Voxel size should shrink after the first potion fill.");
            return;
        }

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));

        InteractionResult secondResult = voxel.interact(player, InteractionHand.MAIN_HAND);
        if (!secondResult.consumesAction()) {
            helper.fail("Second potion interaction should consume the action.");
            return;
        }

        helper.runAfterDelay(1, () -> {
            ItemStack secondHeld = player.getItemInHand(InteractionHand.MAIN_HAND);
            PotionContents secondContents = secondHeld.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (!secondHeld.is(Items.POTION) || !secondContents.is(Potions.WATER)) {
                helper.fail("Second interaction should yield another water potion.");
                return;
            }
            if (voxel.isAlive()) {
                helper.fail("Voxel should be removed after two potion fills at default size.");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelTurnsSourceLavaToObsidian(GameTestHelper helper) {
        BlockPos lavaRelativePos = new BlockPos(2, 0, 2);
        helper.setBlock(lavaRelativePos, Blocks.LAVA.defaultBlockState());

        ServerLevel level = helper.getLevel();
        BlockPos lavaPos = helper.absolutePos(lavaRelativePos);
        BlockPos spawnPos = helper.absolutePos(lavaRelativePos.above(2));

        WaterVoxelEntity voxel = ModEntities.WATER_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create WaterVoxelEntity for lava source test.");
            return;
        }

        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, new Vec3(0.0, -0.25, 0.0), 200);

        AtomicBoolean voxelSeenBeforeImpact = new AtomicBoolean(voxel.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            voxel,
            voxelSeenBeforeImpact,
            200,
            () -> {
                BlockState state = helper.getLevel().getBlockState(lavaPos);
                if (!state.is(Blocks.OBSIDIAN)) {
                    helper.fail("Lava source should convert to obsidian after water voxel collision.");
                    return;
                }
                helper.succeed();
            },
            () -> helper.fail("Water voxel never collided with the lava source within the allotted time."),
            () -> helper.fail("Water voxel must exist before impact to validate lava source conversion.")
        );
    }

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelTurnsFlowingLavaToCobblestone(GameTestHelper helper) {
        BlockPos lavaRelativePos = new BlockPos(2, 0, 2);
        helper.setBlock(
            lavaRelativePos,
            Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 3)
        );

        ServerLevel level = helper.getLevel();
        BlockPos lavaPos = helper.absolutePos(lavaRelativePos);
        BlockPos spawnPos = helper.absolutePos(lavaRelativePos.above(2));

        WaterVoxelEntity voxel = ModEntities.WATER_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create WaterVoxelEntity for flowing lava test.");
            return;
        }

        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, new Vec3(0.0, -0.25, 0.0), 200);

        AtomicBoolean voxelSeenBeforeImpact = new AtomicBoolean(voxel.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            voxel,
            voxelSeenBeforeImpact,
            200,
            () -> {
                BlockState state = helper.getLevel().getBlockState(lavaPos);
                if (!state.is(Blocks.COBBLESTONE)) {
                    helper.fail("Flowing lava should convert to cobblestone after water voxel collision.");
                    return;
                }
                helper.succeed();
            },
            () -> helper.fail("Water voxel never collided with the flowing lava within the allotted time."),
            () -> helper.fail("Water voxel must exist before impact to validate flowing lava conversion.")
        );
    }

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelHotBiomeEvaporationAtZeroWaterPower(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WaterVoxelEntity voxel = ModEntities.WATER_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create WaterVoxelEntity for hot biome evaporation test.");
            return;
        }

        voxel.setForceHotEnvironment(true);
        voxel.setCasterWaterPower(0.0f);
        voxel.setNoGravityCustom(true);

        BlockPos spawnPos = helper.absolutePos(new BlockPos(2, 1, 2));
        float initialSize = voxel.getSize();
        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, Vec3.ZERO, 200);

        helper.runAfterDelay(25, () -> {
            float expectedSize = initialSize * 0.95f;
            float actualSize = voxel.getSize();
            if (Math.abs(actualSize - expectedSize) > 0.0001f) {
                helper.fail("Water voxel should shrink by 5% in hot biome at water power 0. Expected " + expectedSize + " but was " + actualSize + ".");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelHotBiomeEvaporationAtOneWaterPower(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WaterVoxelEntity voxel = ModEntities.WATER_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create WaterVoxelEntity for hot biome evaporation test at power 1.");
            return;
        }

        voxel.setForceHotEnvironment(true);
        voxel.setCasterWaterPower(1.0f);
        voxel.setNoGravityCustom(true);

        BlockPos spawnPos = helper.absolutePos(new BlockPos(2, 1, 2));
        float initialSize = voxel.getSize();
        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, Vec3.ZERO, 200);

        helper.runAfterDelay(25, () -> {
            float expectedSize = initialSize * 0.975f;
            float actualSize = voxel.getSize();
            if (Math.abs(actualSize - expectedSize) > 0.0001f) {
                helper.fail("Water voxel should shrink by 2.5% in hot biome at water power 1. Expected " + expectedSize + " but was " + actualSize + ".");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelHotBiomeNoEvaporationAtHighWaterPower(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WaterVoxelEntity voxel = ModEntities.WATER_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create WaterVoxelEntity for hot biome evaporation test at high power.");
            return;
        }

        voxel.setForceHotEnvironment(true);
        voxel.setCasterWaterPower(3.0f);
        voxel.setNoGravityCustom(true);

        BlockPos spawnPos = helper.absolutePos(new BlockPos(2, 1, 2));
        float initialSize = voxel.getSize();
        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, Vec3.ZERO, 200);

        helper.runAfterDelay(25, () -> {
            float actualSize = voxel.getSize();
            if (Math.abs(actualSize - initialSize) > 0.0001f) {
                helper.fail("Water voxel should not shrink in hot biome at water power 2 or greater. Expected " + initialSize + " but was " + actualSize + ".");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelEvaporatesInstantlyInNether(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        if (nether == null) {
            helper.fail("Nether level is unavailable for testing.");
            return;
        }

        BlockPos relativePos = new BlockPos(2, 1, 2);
        BlockPos spawnPos = helper.absolutePos(relativePos);
        nether.getChunkAt(spawnPos);
        nether.setBlock(spawnPos, Blocks.AIR.defaultBlockState(), 3);

        WaterVoxelEntity voxel = ModEntities.WATER_VOXEL_ENTITY.get().create(nether);
        if (voxel == null) {
            helper.fail("Failed to create WaterVoxelEntity for Nether evaporation test.");
            return;
        }

        voxel.setCasterWaterPower(5.0f);
        voxel.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        voxel.setLifetime(200);
        nether.addFreshEntity(voxel);

        helper.runAfterDelay(2, () -> {
            if (voxel.isAlive()) {
                helper.fail("Water voxel should evaporate instantly in the Nether regardless of water power.");
                return;
            }
            helper.succeed();
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

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void waterVoxelIncreasesCauldronLevel(GameTestHelper helper) {
        BlockPos relativeCauldronPos = new BlockPos(2, 0, 2);
        VoxelTestUtils.prepareColumn(
            helper,
            relativeCauldronPos,
            Blocks.CAULDRON.defaultBlockState(),
            Blocks.GRASS_BLOCK.defaultBlockState(),
            Blocks.AIR.defaultBlockState()
        );

        ServerLevel level = helper.getLevel();
        BlockPos cauldronPos = helper.absolutePos(relativeCauldronPos);
        BlockPos spawnPos = helper.absolutePos(relativeCauldronPos.above(2));

        WaterVoxelEntity voxel = ModEntities.WATER_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create WaterVoxelEntity for test setup.");
            return;
        }

        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, Vec3.ZERO, 200);

        AtomicBoolean voxelSeenBeforeImpact = new AtomicBoolean(voxel.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            voxel,
            voxelSeenBeforeImpact,
            200,
            () -> {
                BlockState state = helper.getLevel().getBlockState(cauldronPos);
                if (!state.is(Blocks.WATER_CAULDRON)) {
                    helper.fail("Water voxel should convert the cauldron to a water cauldron.");
                    return;
                }
                if (!state.hasProperty(LayeredCauldronBlock.LEVEL) || state.getValue(LayeredCauldronBlock.LEVEL) != 1) {
                    helper.fail("Water voxel should add one level to the cauldron.");
                    return;
                }
                helper.succeed();
            },
            () -> helper.fail("Water voxel never collided with the cauldron within the allotted time."),
            () -> helper.fail("Water voxel must exist before impact to validate cauldron filling.")
        );
    }
}
