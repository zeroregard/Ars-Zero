package com.github.ars_zero.common.glyph.convergence;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.spell.ISubsequentEffectProvider;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.registry.ModParticleTimelines;
import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.SoundProperty;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.TileCaster;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectExplosion;
import com.github.ars_zero.common.block.MultiphaseSpellTurretTile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class EffectConvergence extends AbstractEffect implements ISubsequentEffectProvider {

  public static final String ID = "effect_convergence";
  public static final EffectConvergence INSTANCE = new EffectConvergence();

  private static final ResourceLocation[] SUBSEQUENT_GLYPHS = new ResourceLocation[] {
      EffectExplosion.INSTANCE.getRegistryName(),
      EffectConjureWater.INSTANCE.getRegistryName()
  };

  public EffectConvergence() {
    super(ArsZero.prefix(ID), "Convergence");
  }

  @Override
  public ResourceLocation[] getSubsequentEffectGlyphs() {
    return SUBSEQUENT_GLYPHS;
  }

  @Override
  public void onResolve(HitResult rayTraceResult, Level world, @Nullable LivingEntity shooter, SpellStats spellStats,
      SpellContext spellContext, SpellResolver resolver) {
    if (world.isClientSide || !(world instanceof ServerLevel serverLevel)) {
      return;
    }

    Vec3 pos = safelyGetHitPos(rayTraceResult);

    if (hasEffect(spellContext, EffectExplosion.class)) {
      ExplosionConvergenceHelper.handleExplosionConvergence(serverLevel, pos, shooter, spellStats, spellContext,
          this);
    } else if (hasEffect(spellContext, EffectConjureWater.class)) {
      WaterConvergenceHelper.handleWaterConvergence(serverLevel, pos, shooter, spellContext, this);
    } else if (rayTraceResult instanceof EntityHitResult entityHitResult) {
      ChargerHelper.handlePlayerCharger(serverLevel, pos, entityHitResult, shooter, spellContext, this);
    } else if (rayTraceResult instanceof BlockHitResult blockHitResult) {
      ChargerHelper.handleBlockCharger(serverLevel, pos, blockHitResult, shooter, spellContext, this);
    }
  }

  void updateTemporalContext(LivingEntity shooter, Entity entity, SpellContext spellContext) {
    if (!(shooter instanceof Player player)) {
      return;
    }

    MultiPhaseCastContext context = null;
    if (spellContext.getCaster() instanceof TileCaster tileCaster && tileCaster.getTile() instanceof MultiphaseSpellTurretTile turretTile) {
      context = turretTile.getCastContext();
    }
    if (context == null) {
      ItemStack casterTool = spellContext.getCasterTool();
      context = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
    }
    if (context == null) {
      return;
    }

    SpellResult entityResult = SpellResult.fromHitResultWithCaster(
        new EntityHitResult(entity),
        SpellEffectType.RESOLVED,
        player);

    context.beginResults.clear();
    context.beginResults.add(entityResult);
  }

  private boolean hasEffect(SpellContext context, Class<? extends AbstractEffect> effectClass) {
    SpellContext iterator = context.clone();
    while (iterator.hasNextPart()) {
      AbstractSpellPart next = iterator.nextPart();
      if (effectClass.isInstance(next)) {
        return true;
      }
    }
    return false;
  }

  void consumeFirstConjureWaterEffect(SpellContext context) {
    SpellContext iterator = context.clone();
    while (iterator.hasNextPart()) {
      AbstractSpellPart next = iterator.nextPart();
      if (next instanceof EffectConjureWater conjureWater) {
        consumeEffect(context, conjureWater);
        return;
      }
    }
  }

  void consumeEffect(SpellContext context, AbstractEffect targetEffect) {
    ResourceLocation targetId = targetEffect.getRegistryName();
    while (context.hasNextPart()) {
      AbstractSpellPart consumed = context.nextPart();
      if (consumed instanceof AbstractEffect consumedEffect
          && effectsMatch(consumedEffect, targetEffect, targetId)) {
        break;
      }
    }
  }

  private boolean effectsMatch(AbstractEffect candidate, AbstractEffect reference, ResourceLocation id) {
    if (candidate == reference) {
      return true;
    }
    ResourceLocation candidateId = candidate.getRegistryName();
    return id != null && id.equals(candidateId);
  }

  @Override
  public int getDefaultManaCost() {
    return 200;
  }

  @Override
  public SpellTier defaultTier() {
    return SpellTier.THREE;
  }

  @NotNull
  @Override
  public Set<AbstractAugment> getCompatibleAugments() {
    return Set.of();
  }

  @Override
  public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
    super.addAugmentDescriptions(map);
  }

  @Override
  public String getBookDescription() {
    return "Creates a convergence point that can be augmented with other effects.";
  }

  @NotNull
  @Override
  public Set<SpellSchool> getSchools() {
    return Set.of(SpellSchools.MANIPULATION);
  }

  @Nullable
  SoundEvent getResolveSoundFromStyle(SpellContext spellContext) {
    var timeline = spellContext.getParticleTimeline(ModParticleTimelines.CONVERGENCE_TIMELINE.get());
    SoundProperty resolveSound = timeline.resolveSound();
    if (resolveSound != null && resolveSound.sound != null) {
      var spellSound = resolveSound.sound.getSound();
      if (spellSound != null) {
        var soundEventHolder = spellSound.getSoundEvent();
        if (soundEventHolder != null) {
          return soundEventHolder.value();
        }
      }
    }
    return null;
  }

  @Nullable
  SoundEvent getWarningSoundFromStyle(SpellContext spellContext) {
    return getResolveSoundFromStyle(spellContext);
  }

  void triggerResolveEffects(SpellContext spellContext, Level level, Vec3 position) {
    if (level == null) {
      return;
    }
    var timeline = spellContext.getParticleTimeline(ModParticleTimelines.CONVERGENCE_TIMELINE.get());
    TimelineEntryData entryData = timeline.onResolvingEffect();
    ParticleEmitter particleEmitter = createStaticEmitter(entryData, position);
    particleEmitter.tick(level);
  }
}
