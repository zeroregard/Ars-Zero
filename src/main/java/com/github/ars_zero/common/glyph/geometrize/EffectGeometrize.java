package com.github.ars_zero.common.glyph.geometrize;

import alexthw.ars_elemental.common.glyphs.EffectConjureTerrain;
import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.glyph.augment.AugmentCube;
import com.github.ars_zero.common.glyph.augment.AugmentFlatten;
import com.github.ars_zero.common.glyph.augment.AugmentHollow;
import com.github.ars_zero.common.glyph.augment.AugmentSphere;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAccelerate;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDecelerate;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.github.ars_zero.common.shape.GeometryDescription;
import com.github.ars_zero.common.spell.ISubsequentEffectProvider;
import com.github.ars_zero.common.spell.TemporalContextRecorder;
import com.github.ars_zero.registry.ModParticleTimelines;
import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.SoundProperty;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectBreak;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class EffectGeometrize extends AbstractEffect implements ISubsequentEffectProvider {

  public static final String ID = "effect_geometrize";
  public static final EffectGeometrize INSTANCE = new EffectGeometrize();

  private static final ResourceLocation[] SUBSEQUENT_GLYPHS = new ResourceLocation[] {
      EffectConjureTerrain.INSTANCE.getRegistryName(),
      EffectBreak.INSTANCE.getRegistryName()
  };

  public ModConfigSpec.IntValue MAX_SIZE;
  public ModConfigSpec.DoubleValue GEOMETRY_ENTITY_GENERIC_RESOLVER_MANA_COST_MULTIPLIER;

  public EffectGeometrize() {
    super(ArsZero.prefix(ID), "Geometrize");
  }

  @Override
  public void buildConfig(ModConfigSpec.Builder builder) {
    super.buildConfig(builder);
    builder.comment("Geometrize Effect Settings").push("geometrize");
    MAX_SIZE = builder.comment(
        "Maximum size for geometry structures.",
        "Size is the structure edge length in blocks. Default is 32.")
        .defineInRange("maxSize", 32, 1, 128);
    GEOMETRY_ENTITY_GENERIC_RESOLVER_MANA_COST_MULTIPLIER = builder.comment(
        "Mana cost multiplier for generic geometry entity resolver.",
        "Each block resolved by the generic geometry entity costs this percentage of the forwarded spell's total mana cost.",
        "Default is 0.05 (5%).")
        .defineInRange("geometryEntityGenericResolverManaCostMultiplier", 0.05, 0.0, 1.0);
    builder.pop();
  }

  public int getMaxSize() {
    if (MAX_SIZE == null) {
      return 32;
    }
    return MAX_SIZE.get();
  }

  public double getGenericResolverManaCostMultiplier() {
    if (GEOMETRY_ENTITY_GENERIC_RESOLVER_MANA_COST_MULTIPLIER == null) {
      return 0.05;
    }
    return GEOMETRY_ENTITY_GENERIC_RESOLVER_MANA_COST_MULTIPLIER.get();
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

    AbstractEffect firstEffect = findFirstGeometryEffect(spellContext);
    if (firstEffect instanceof EffectConjureTerrain) {
      GeometrizeTerrainHelper.handleConjureTerrain(serverLevel, pos, shooter, spellContext, this,
          rayTraceResult, resolver);
    } else if (firstEffect instanceof EffectBreak breakEffect) {
      GeometrizeBreakHelper.handleBreak(serverLevel, pos, shooter, spellContext, this, rayTraceResult,
          breakEffect, resolver);
    } else {
      GeometrizeMageBlockHelper.handleMageBlock(serverLevel, pos, shooter, spellContext, this,
          rayTraceResult, resolver);
    }
  }

  @Nullable
  private AbstractEffect findFirstGeometryEffect(SpellContext context) {
    SpellContext iterator = context.clone();

    while (iterator.hasNextPart()) {
      AbstractSpellPart next = iterator.nextPart();

      if (next instanceof AbstractEffect effect) {
        if (effect instanceof EffectConjureTerrain) {
          return effect;
        } else if (effect instanceof EffectBreak) {
          return effect;
        }
      }
    }

    return null;
  }

  public void consumeEffect(SpellContext context, AbstractEffect targetEffect) {
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
    return 100;
  }

  @Override
  public SpellTier defaultTier() {
    return SpellTier.THREE;
  }

  @NotNull
  @Override
  public Set<AbstractAugment> getCompatibleAugments() {
    return augmentSetOf(
        AugmentHollow.INSTANCE,
        AugmentSphere.INSTANCE,
        AugmentCube.INSTANCE,
        AugmentFlatten.INSTANCE,
        AugmentSensitive.INSTANCE,
        AugmentAccelerate.INSTANCE,
        AugmentDecelerate.INSTANCE);
  }

  @Override
  protected void addDefaultAugmentLimits(Map<ResourceLocation, Integer> defaults) {
    super.addDefaultAugmentLimits(defaults);
    defaults.put(AugmentHollow.INSTANCE.getRegistryName(), 1);
    defaults.put(AugmentSphere.INSTANCE.getRegistryName(), 1);
    defaults.put(AugmentCube.INSTANCE.getRegistryName(), 1);
    defaults.put(AugmentFlatten.INSTANCE.getRegistryName(), 1);
  }

  @Override
  public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
    super.addAugmentDescriptions(map);
    map.put(AugmentHollow.INSTANCE,
        "Generates hollow shapes, placing only the outer shell.");
    map.put(AugmentSphere.INSTANCE,
        "Generates spherical shapes. When flattened, produces circles.");
    map.put(AugmentCube.INSTANCE,
        "Generates cube shapes (default). When flattened, produces squares.");
    map.put(AugmentFlatten.INSTANCE,
        "Projects 3D shapes into 2D based on the caster's look direction.");
    map.put(AugmentSensitive.INSTANCE,
        "Processes blocks from bottom to top instead of top to bottom.");
    map.put(AugmentAccelerate.INSTANCE,
        "Increases the casting speed of the Golem. Speeding up makes the Golem faster.");
    map.put(AugmentDecelerate.INSTANCE,
        "Decreases the casting speed of the Golem. Slowing down makes it slower.");
  }

  public GeometryDescription resolveGeometryDescription(SpellContext context, @Nullable LivingEntity caster) {
    return GeometrizeCompatibilityHelper.resolveGeometryDescription(context, caster);
  }

  @Override
  public String getBookDescription() {
    return "Creates geometric structures using subsequent effects. Combine with Conjure Terrain to build, or Break to demolish. Use geometry augments (Sphere, Cube, Hollow, Flatten) to control the shape.";
  }

  @NotNull
  @Override
  public Set<SpellSchool> getSchools() {
    return Set.of(SpellSchools.ELEMENTAL_EARTH);
  }

  @Nullable
  SoundEvent getResolveSoundFromStyle(SpellContext spellContext) {
    var timeline = spellContext.getParticleTimeline(ModParticleTimelines.GEOMETRIZE_TIMELINE.get());
    if (timeline == null) {
      return null;
    }
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

  public void triggerResolveEffects(SpellContext spellContext, Level level, Vec3 position) {
    if (level == null) {
      return;
    }
    var timeline = spellContext.getParticleTimeline(ModParticleTimelines.GEOMETRIZE_TIMELINE.get());
    if (timeline == null) {
      return;
    }
    TimelineEntryData entryData = timeline.onResolvingEffect();
    ParticleEmitter particleEmitter = createStaticEmitter(entryData, position);
    particleEmitter.tick(level);
  }
}
