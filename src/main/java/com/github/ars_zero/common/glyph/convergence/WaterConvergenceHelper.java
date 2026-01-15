package com.github.ars_zero.common.glyph.convergence;

import com.github.ars_zero.common.entity.water.WaterConvergenceControllerEntity;
import com.github.ars_zero.registry.ModEntities;
import com.github.ars_zero.common.spell.SpellAugmentExtractor;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class WaterConvergenceHelper {

  private static final int DEFAULT_LIFESPAN = 1;
  private static final double BASE_WATER_RADIUS = ConvergenceConstants.BASE_CONVERGENCE_RADIUS + 2.0;

  private WaterConvergenceHelper() {
  }

  public static void handleWaterConvergence(ServerLevel serverLevel, Vec3 pos, @Nullable LivingEntity shooter,
      SpellContext spellContext, EffectConvergence convergence) {
    Vec3 airPos = findNearestAirBlock(serverLevel, pos, 4);
    if (airPos == null) {
      return;
    }

    WaterConvergenceControllerEntity entity = new WaterConvergenceControllerEntity(
        ModEntities.WATER_CONVERGENCE_CONTROLLER.get(), serverLevel);
    entity.setPos(airPos.x, airPos.y, airPos.z);
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
    spellContext.setCanceled(true);
    convergence.triggerResolveEffects(spellContext, serverLevel, airPos);
  }

  @Nullable
  private static Vec3 findNearestAirBlock(ServerLevel level, Vec3 center, int maxRadius) {
    BlockPos centerPos = BlockPos.containing(center);

    for (int radius = 0; radius <= maxRadius; radius++) {
      int radiusSquared = radius * radius;

      for (int dy = -radius; dy <= radius; dy++) {
        for (int dx = -radius; dx <= radius; dx++) {
          for (int dz = -radius; dz <= radius; dz++) {
            int distSquared = dx * dx + dy * dy + dz * dz;
            if (distSquared > radiusSquared) {
              continue;
            }

            BlockPos checkPos = centerPos.offset(dx, dy, dz);
            if (!level.isLoaded(checkPos)) {
              continue;
            }

            BlockState state = level.getBlockState(checkPos);
            if (state.isAir() || state.is(Blocks.WATER)) {
              return Vec3.atCenterOf(checkPos);
            }
          }
        }
      }
    }

    return null;
  }

  private static int calculateWaterRadius(int aoeLevel, int dampenLevel) {
    double radius = BASE_WATER_RADIUS + aoeLevel - 0.5 * dampenLevel;
    return (int) Math.max(1.0, Math.round(radius));
  }
}
