package com.github.ars_zero.common.glyph.convergence;

import com.github.ars_zero.common.entity.water.WaterConvergenceControllerEntity;
import com.github.ars_zero.registry.ModEntities;
import com.github.ars_zero.common.spell.SpellAugmentExtractor;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class WaterConvergenceHelper {

  private static final int DEFAULT_LIFESPAN = 1;
  private static final double BASE_WATER_RADIUS = ConvergenceConstants.BASE_CONVERGENCE_RADIUS + 2.0;

  private WaterConvergenceHelper() {
  }

  public static void handleWaterConvergence(ServerLevel serverLevel, Vec3 pos, @Nullable LivingEntity shooter,
      SpellContext spellContext, EffectConvergence convergence) {
    WaterConvergenceControllerEntity entity = new WaterConvergenceControllerEntity(
        ModEntities.WATER_CONVERGENCE_CONTROLLER.get(), serverLevel);
    entity.setPos(pos.x, pos.y, pos.z);
    if (shooter != null) {
      entity.setCaster(shooter);
    }

    SpellContext iterator = spellContext.clone();
    while (iterator.hasNextPart()) {
      AbstractSpellPart next = iterator.nextPart();
      if (next instanceof EffectConjureWater conjureWaterEffect) {
        SpellAugmentExtractor.AugmentData augmentData = SpellAugmentExtractor
            .extractApplicableAugments(spellContext, conjureWaterEffect);

        int radius = calculateWaterRadius(augmentData.aoeLevel, augmentData.dampenLevel);
        entity.setRadius(radius);
        break;
      }
    }

    entity.setLifespan(DEFAULT_LIFESPAN);
    serverLevel.addFreshEntity(entity);
    convergence.updateTemporalContext(shooter, entity, spellContext);
    convergence.consumeFirstConjureWaterEffect(spellContext);
    convergence.triggerResolveEffects(spellContext, serverLevel, pos);
  }

  private static int calculateWaterRadius(int aoeLevel, int dampenLevel) {
    double radius = BASE_WATER_RADIUS + aoeLevel - 0.5 * dampenLevel;
    return (int) Math.max(1.0, Math.round(radius));
  }
}
