package com.github.ars_zero.common.glyph.convergence;

import com.github.ars_zero.common.entity.break_blocks.BreakConvergenceEntity;
import com.github.ars_zero.common.shape.GeometryDescription;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtract;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentFortune;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentRandomize;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectBreak;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class BreakConvergenceHelper {

  private BreakConvergenceHelper() {
  }

  public static void handleBreak(ServerLevel serverLevel, Vec3 pos, @Nullable LivingEntity shooter,
      SpellContext spellContext, EffectConvergence convergence, HitResult rayTraceResult,
      EffectBreak breakEffect, SpellStats spellStats) {
    BlockPos centerBlock = BlockPos.containing(pos);
    Vec3 center = Vec3.atCenterOf(centerBlock);

    GeometryDescription geometryDescription = ConvergenceCompatibilityHelper
        .resolveGeometryDescription(spellContext, shooter);

    int augmentCount = GeometryConvergenceUtils.countAugmentsAfterEffect(spellContext, EffectBreak.class);
    int size = GeometryConvergenceUtils.calculateSize(augmentCount);

    Vec3 offsetCenter = GeometryConvergenceUtils.calculateOffsetPosition(center, rayTraceResult, size,
        geometryDescription);

    BreakConvergenceEntity entity = new BreakConvergenceEntity(
        ModEntities.BREAK_CONVERGENCE_CONTROLLER.get(),
        serverLevel);

    entity.setPos(offsetCenter.x, offsetCenter.y, offsetCenter.z);
    if (shooter != null) {
      entity.setCaster(shooter);
      if (shooter instanceof Player player) {
        entity.setMarkerPos(player.blockPosition());
      }
    }
    entity.setLifespan(GeometryConvergenceUtils.DEFAULT_LIFESPAN);
    entity.setGeometryDescription(geometryDescription);
    entity.setSize(size);
    entity.setBasePosition(offsetCenter);

    int amplify = spellStats.getBuffCount(AugmentAmplify.INSTANCE);
    int dampen = spellStats.getBuffCount(AugmentDampen.INSTANCE);
    int fortune = spellStats.getBuffCount(AugmentFortune.INSTANCE);
    int extract = spellStats.getBuffCount(AugmentExtract.INSTANCE);
    int randomize = spellStats.getBuffCount(AugmentRandomize.INSTANCE);
    boolean sensitive = spellStats.isSensitive();
    entity.setSpellStats(amplify, dampen, fortune, extract, randomize, sensitive);

    serverLevel.addFreshEntity(entity);
    convergence.updateTemporalContext(shooter, entity, spellContext);
    convergence.consumeEffect(spellContext, breakEffect);
    spellContext.setCanceled(true);
    convergence.triggerResolveEffects(spellContext, serverLevel, center);
  }
}
