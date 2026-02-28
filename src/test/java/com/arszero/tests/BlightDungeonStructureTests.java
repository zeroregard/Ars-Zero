package com.arszero.tests;

import com.github.ars_zero.ArsZero;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

/**
 * Game test that loads and places the blight dungeon entrance template, then asserts
 * that blocks were placed. Used to verify structure NBT loads (GZIP, layout) and places correctly.
 */
@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlightDungeonStructureTests {

    private static final ResourceLocation ENTRANCE_TEMPLATE = ResourceLocation.fromNamespaceAndPath(ArsZero.MOD_ID, "blight_dungeon/entrance");
    private static final ResourceLocation SIMPLE_3X3_TEMPLATE = ResourceLocation.fromNamespaceAndPath(ArsZero.MOD_ID, "test/simple_3x3");

    private BlightDungeonStructureTests() {
    }

    public static void registerGameTests(RegisterGameTestsEvent event) {
        if (TestRegistrationFilter.shouldRegister(BlightDungeonStructureTests.class)) {
            event.register(BlightDungeonStructureTests.class);
        }
    }

    @GameTest(batch = "BlightDungeonStructureTests", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void blightDungeonEntranceTemplateLoadsAndPlaces(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplateManager manager = level.getServer().getStructureManager();
        Optional<StructureTemplate> opt = manager.get(ENTRANCE_TEMPLATE);
        if (opt.isEmpty()) {
            String msg = "Failed to load structure template: " + ENTRANCE_TEMPLATE + " (missing or invalid NBT?)";
            ArsZero.LOGGER.error("[BlightDungeon test] {}", msg);
            helper.fail(msg);
            return;
        }
        StructureTemplate template = opt.get();
        if (template.getSize().getX() == 0 && template.getSize().getY() == 0 && template.getSize().getZ() == 0) {
            String msg = "Structure template has zero size (likely failed to parse NBT).";
            ArsZero.LOGGER.error("[BlightDungeon test] {}", msg);
            helper.fail(msg);
            return;
        }
        BlockPos placeAt = helper.absolutePos(new BlockPos(1, 1, 1));
        boolean placed = template.placeInWorld(level, placeAt, placeAt, new StructurePlaceSettings(), level.getRandom(), 2);
        if (!placed) {
            String msg = "placeInWorld returned false.";
            ArsZero.LOGGER.error("[BlightDungeon test] {}", msg);
            helper.fail(msg);
            return;
        }
        if (level.getBlockState(placeAt).isAir()) {
            String msg = "Expected a block at placement origin (template may be empty or malformed).";
            ArsZero.LOGGER.error("[BlightDungeon test] {}", msg);
            helper.fail(msg);
        }
        helper.succeed();
    }

    @GameTest(batch = "BlightDungeonStructureTests", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void simple3x3TemplateLoadsAndPlaces(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplateManager manager = level.getServer().getStructureManager();
        Optional<StructureTemplate> opt = manager.get(SIMPLE_3X3_TEMPLATE);
        if (opt.isEmpty()) {
            String msg = "Failed to load structure template: " + SIMPLE_3X3_TEMPLATE + " (missing or invalid NBT?)";
            ArsZero.LOGGER.error("[BlightDungeon test] {}", msg);
            helper.fail(msg);
            return;
        }
        StructureTemplate template = opt.get();
        if (template.getSize().getX() != 3 || template.getSize().getY() != 3 || template.getSize().getZ() != 3) {
            helper.fail("Expected size 3x3x3, got " + template.getSize().getX() + "x" + template.getSize().getY() + "x" + template.getSize().getZ());
            return;
        }
        BlockPos placeAt = helper.absolutePos(new BlockPos(1, 1, 1));
        boolean placed = template.placeInWorld(level, placeAt, placeAt, new StructurePlaceSettings(), level.getRandom(), 2);
        if (!placed) {
            helper.fail("placeInWorld returned false.");
            return;
        }
        if (level.getBlockState(placeAt).isAir()) {
            helper.fail("Expected a block at placement origin (template may be empty or malformed).");
            return;
        }
        helper.succeed();
    }
}
