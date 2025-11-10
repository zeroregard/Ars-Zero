package com.arszero.tests;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ArcaneVoxelEntity;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.ANFakePlayer;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectExplosion;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsZero.MOD_ID)
@PrefixGameTestTemplate(false)
public class ArcaneVoxelTests {
    public static void registerGameTests(RegisterGameTestsEvent event) {
        event.register(ArcaneVoxelTests.class);
    }

    @GameTest(templateNamespace = ArsZero.MOD_ID, template = "common/empty_7x7")
    public static void arcaneVoxelExplodesBlock(GameTestHelper helper) {
        BlockPos relativeTargetPos = new BlockPos(2, 0, 2);
        VoxelTestUtils.prepareColumn(
            helper,
            relativeTargetPos,
            Blocks.DIRT.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(),
            Blocks.AIR.defaultBlockState()
        );

        ServerLevel level = helper.getLevel();
        BlockPos absoluteTargetPos = helper.absolutePos(relativeTargetPos);
        BlockPos spawnPos = helper.absolutePos(relativeTargetPos.above(3));

        ArcaneVoxelEntity voxel = ModEntities.ARCANE_VOXEL_ENTITY.get().create(level);
        if (voxel == null) {
            helper.fail("Failed to create ArcaneVoxelEntity for test setup.");
            return;
        }

        LivingEntity caster = ANFakePlayer.getPlayer(level);
        caster.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);

        IManaCap manaCap = CapabilityRegistry.getMana(caster);
        if (manaCap != null) {
            manaCap.setMaxMana(1000);
            manaCap.setMana(1000);
        }

        Spell explosionSpell = new Spell(EffectExplosion.INSTANCE);
        SpellContext context = new SpellContext(level, explosionSpell, caster, LivingCaster.from(caster));
        context.setCasterTool(ItemStack.EMPTY);

        SpellResolver resolver = new SpellResolver(context).withSilent(true);

        voxel.setResolver(resolver);
        voxel.setCaster(caster);

        VoxelTestUtils.spawnVoxel(helper, voxel, spawnPos, new Vec3(0.0D, -0.45D, 0.0D), 200);

        AtomicBoolean voxelSeenBeforeImpact = new AtomicBoolean(voxel.isAlive());
        VoxelTestUtils.awaitVoxelRemoval(
            helper,
            voxel,
            voxelSeenBeforeImpact,
            200,
            () -> {
                BlockState state = helper.getLevel().getBlockState(absoluteTargetPos);
                if (!state.isAir()) {
                    helper.fail("Target block should be destroyed by the arcane voxel explosion.");
                    return;
                }
                helper.succeed();
            },
            () -> helper.fail("Arcane voxel never reached the target block within the allotted time."),
            () -> helper.fail("Arcane voxel must exist before impact to validate explosion behavior.")
        );
    }
}

