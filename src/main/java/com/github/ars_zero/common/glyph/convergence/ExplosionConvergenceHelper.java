package com.github.ars_zero.common.glyph.convergence;

import com.github.ars_zero.common.entity.explosion.ExplosionControllerEntity;
import com.github.ars_zero.common.spell.TemporalContextRecorder;
import com.github.ars_zero.registry.ModEntities;
import com.github.ars_zero.common.spell.SpellAugmentExtractor;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectExplosion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ExplosionConvergenceHelper {

  private static final int DEFAULT_LIFESPAN = 20;

  private ExplosionConvergenceHelper() {
  }

  public static void handleExplosionConvergence(ServerLevel serverLevel, Vec3 pos, @Nullable LivingEntity shooter,
      SpellStats spellStats, SpellContext spellContext, SpellResolver resolver, EffectConvergence convergence) {
    ExplosionControllerEntity entity = new ExplosionControllerEntity(ModEntities.EXPLOSION_CONTROLLER.get(),
        serverLevel);
    entity.setPos(pos.x, pos.y, pos.z);

    SpellContext iterator = spellContext.clone();
    while (iterator.hasNextPart()) {
      AbstractSpellPart next = iterator.nextPart();
      if (next instanceof EffectExplosion explosionEffect) {
        SpellAugmentExtractor.AugmentData augmentData = SpellAugmentExtractor
            .extractApplicableAugments(spellContext, explosionEffect);

        double intensity = calculateExplosionIntensity(spellStats);
        float baseDamage = EffectExplosion.INSTANCE.DAMAGE.get().floatValue();
        float powerMultiplier = EffectExplosion.INSTANCE.AMP_DAMAGE.get().floatValue();

        entity.setExplosionParams(intensity, baseDamage, powerMultiplier, augmentData.aoeLevel,
            augmentData.amplifyLevel, augmentData.dampenLevel);
        entity.setLifespan(DEFAULT_LIFESPAN);

        SoundEvent resolveSound = convergence.getResolveSoundFromStyle(spellContext);
        entity.setResolveSound(resolveSound);

        serverLevel.addFreshEntity(entity);
        TemporalContextRecorder.record(spellContext, entity);
        convergence.consumeEffect(spellContext, explosionEffect);
        convergence.triggerResolveEffects(spellContext, serverLevel, pos);
        break;
      }
    }
  }

  private static double calculateExplosionIntensity(SpellStats spellStats) {
    double base = EffectExplosion.INSTANCE.BASE.get();
    double ampValue = EffectExplosion.INSTANCE.AMP_VALUE.get();
    double aoeBonus = EffectExplosion.INSTANCE.AOE_BONUS.get();

    double intensity = base + ampValue * spellStats.getAmpMultiplier() + aoeBonus * spellStats.getAoeMultiplier();
    int dampen = spellStats.getBuffCount(AugmentDampen.INSTANCE);
    intensity -= 0.5 * dampen;

    return Math.max(0.0, intensity);
  }
}
