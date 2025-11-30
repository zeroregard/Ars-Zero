package com.arszero.tests;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.block.PhasedSpellTurretTile;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.registry.ModBlocks;
import com.hollingsworth.arsnouveau.common.block.BasicSpellTurret;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectBreak;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectLight;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;
import java.util.List;

@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public class PhasedSpellTurretTests {

    public static void registerGameTests(RegisterGameTestsEvent event) {
        if (TestRegistrationFilter.shouldRegister(PhasedSpellTurretTests.class)) {
            event.register(PhasedSpellTurretTests.class);
        }
    }

    @GameTest(batch = "PhasedSpellTurretTests", templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void phasedTurretCastsBeginTickEnd(GameTestHelper helper) {
        BlockPos turretPos = new BlockPos(3, 1, 3);
        BlockPos turretBase = turretPos.below();
        helper.setBlock(turretBase, Blocks.SMOOTH_STONE.defaultBlockState());
        helper.setBlock(turretPos, ModBlocks.PHASED_SPELL_TURRET.get().defaultBlockState().setValue(BasicSpellTurret.FACING, Direction.SOUTH));
        PhasedSpellTurretTile tile = helper.getBlockEntity(turretPos) instanceof PhasedSpellTurretTile phased ? phased : null;
        if (tile == null) {
            helper.fail("Expected PhasedSpellTurretTile at test position.");
            return;
        }
        UUID owner = helper.makeMockPlayer(GameType.SURVIVAL).getUUID();
        Spell beginSpell = new Spell(MethodTouch.INSTANCE, EffectBreak.INSTANCE).withName("Begin");
        Spell tickSpell = new Spell(MethodTouch.INSTANCE, EffectLight.INSTANCE).withName("Tick");
        Spell endSpell = new Spell(MethodTouch.INSTANCE, EffectBreak.INSTANCE).withName("End");
        tile.configureSpells(beginSpell, tickSpell, endSpell, owner);
        BlockPos jarPos = turretPos.east();
        helper.setBlock(jarPos.below(), Blocks.SMOOTH_STONE.defaultBlockState());
        helper.setBlock(jarPos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("ars_nouveau", "source_jar")).defaultBlockState());
        if (helper.getBlockEntity(jarPos) instanceof SourceJarTile jar) {
            jar.setSource(jar.getMaxSource());
            jar.updateBlock();
        }
        BlockPos targetPos = turretPos.relative(Direction.SOUTH);
        helper.setBlock(targetPos, Blocks.SMOOTH_STONE.defaultBlockState());
        BlockPos signalPos = turretBase;
        helper.runAtTickTime(1, () -> helper.setBlock(signalPos, Blocks.REDSTONE_BLOCK.defaultBlockState()));
        helper.runAtTickTime(11, () -> helper.setBlock(signalPos, Blocks.SMOOTH_STONE.defaultBlockState()));
        helper.runAtTickTime(25, () -> {
            List<PhasedSpellTurretTile.PhaseExecution> history = tile.getPhaseHistory();
            if (history.isEmpty()) {
                helper.fail("Phased turret never recorded a cast.");
                return;
            }
            if (history.get(0).phase() != AbstractMultiPhaseCastDevice.Phase.BEGIN) {
                helper.fail("First recorded phase should be BEGIN.");
                return;
            }
            long tickCount = history.stream().filter(record -> record.phase() == AbstractMultiPhaseCastDevice.Phase.TICK).count();
            if (tickCount != 9) {
                helper.fail("Expected 9 tick casts while powered; got " + tickCount);
                return;
            }
            if (history.get(history.size() - 1).phase() != AbstractMultiPhaseCastDevice.Phase.END) {
                helper.fail("Final recorded phase should be END.");
                return;
            }
            helper.succeed();
        });
    }
}
