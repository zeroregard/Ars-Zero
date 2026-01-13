package com.github.ars_zero.common.glyph.convergence;

import com.github.ars_zero.common.entity.break_blocks.BreakConvergenceEntity;
import com.github.ars_zero.common.shape.GeometryDescription;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtract;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentFortune;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentRandomize;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectBreak;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class BreakConvergenceHelper {

  private BreakConvergenceHelper() {
  }

  public static void handleBreak(ServerLevel serverLevel, Vec3 pos, @Nullable LivingEntity shooter,
      SpellContext spellContext, EffectConvergence convergence, HitResult rayTraceResult,
      EffectBreak breakEffect) {
    BlockPos centerBlock = BlockPos.containing(pos);
    Vec3 center = Vec3.atCenterOf(centerBlock);

    GeometryDescription geometryDescription = ConvergenceCompatibilityHelper
        .resolveGeometryDescription(spellContext, shooter);

    int augmentCount = GeometryConvergenceUtils.countAugmentsAfterEffect(spellContext, EffectBreak.class);
    int size = GeometryConvergenceUtils.getPreferredSize(shooter, augmentCount);
    int depth = GeometryConvergenceUtils.getPreferredDepth(shooter);

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
    entity.setDepth(depth);
    entity.setBasePosition(offsetCenter);

    SpellStats breakStats = computeBreakSpellStats(spellContext, shooter, serverLevel);

    int harvestLevel = BlockUtil.getBaseHarvestLevel(breakStats);
    int fortune = breakStats.getBuffCount(AugmentFortune.INSTANCE);
    int extract = breakStats.getBuffCount(AugmentExtract.INSTANCE);
    int randomize = breakStats.getBuffCount(AugmentRandomize.INSTANCE);
    boolean sensitive = breakStats.isSensitive();
    entity.setSpellStats(harvestLevel, fortune, extract, randomize, sensitive);

    serverLevel.addFreshEntity(entity);
    convergence.updateTemporalContext(shooter, entity, spellContext);
    convergence.consumeEffect(spellContext, breakEffect);
    spellContext.setCanceled(true);
    convergence.triggerResolveEffects(spellContext, serverLevel, center);
  }

  private static SpellStats computeBreakSpellStats(SpellContext spellContext, @Nullable LivingEntity shooter,
      ServerLevel level) {
    List<AbstractAugment> augments = collectAugmentsAfterEffect(spellContext, EffectBreak.class);

    SpellStats stats = new SpellStats.Builder()
        .setAugments(augments)
        .addItemsFromEntity(shooter)
        .build(EffectBreak.INSTANCE, null, level, shooter, spellContext);

    if (shooter != null) {
      MobEffectInstance miningFatigue = shooter.getEffect(MobEffects.DIG_SLOWDOWN);
      if (miningFatigue != null) {
        stats.setAmpMultiplier(stats.getAmpMultiplier() - miningFatigue.getAmplifier());
      }
    }

    return stats;
  }

  private static List<AbstractAugment> collectAugmentsAfterEffect(SpellContext context,
      Class<? extends AbstractSpellPart> effectClass) {
    List<AbstractAugment> augments = new ArrayList<>();
    var recipe = context.getSpell().unsafeList();
    boolean foundEffect = false;

    for (AbstractSpellPart part : recipe) {
      if (!foundEffect) {
        if (effectClass.isInstance(part)) {
          foundEffect = true;
        }
        continue;
      }

      if (part instanceof AbstractEffect) {
        break;
      }

      if (part instanceof AbstractAugment augment) {
        augments.add(augment);
      }
    }

    return augments;
  }
}
