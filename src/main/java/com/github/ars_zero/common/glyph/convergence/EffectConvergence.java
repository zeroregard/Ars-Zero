package com.github.ars_zero.common.glyph.convergence;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.glyph.augment.AugmentCube;
import com.github.ars_zero.common.glyph.augment.AugmentFlatten;
import com.github.ars_zero.common.glyph.augment.AugmentHollow;
import com.github.ars_zero.common.glyph.augment.AugmentSphere;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.shape.GeometryDescription;
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
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectExplosion;
import alexthw.ars_elemental.common.glyphs.EffectConjureTerrain;
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
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class EffectConvergence extends AbstractEffect implements ISubsequentEffectProvider {

  public static final String ID = "effect_convergence";
  public static final EffectConvergence INSTANCE = new EffectConvergence();

  private static final ResourceLocation[] SUBSEQUENT_GLYPHS = new ResourceLocation[] {
      EffectExplosion.INSTANCE.getRegistryName(),
      EffectConjureWater.INSTANCE.getRegistryName(),
      EffectConjureTerrain.INSTANCE.getRegistryName()
  };

  public ModConfigSpec.IntValue TERRAIN_MAX_SIZE;

  public EffectConvergence() {
    super(ArsZero.prefix(ID), "Convergence");
  }

  @Override
  public void buildConfig(ModConfigSpec.Builder builder) {
    super.buildConfig(builder);
    builder.comment("Convergence Effect Settings").push("convergence");
    TERRAIN_MAX_SIZE = builder.comment(
        "Maximum cube size for Conjure Terrain convergence.",
        "Size is the cube edge length in blocks. Default is 32.")
        .defineInRange("terrainMaxSize", 32, 1, 128);
    builder.pop();
  }

  public int getTerrainMaxSize() {
    if (TERRAIN_MAX_SIZE == null) {
      return 32;
    }
    return TERRAIN_MAX_SIZE.get();
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

    AbstractEffect firstEffect = findFirstConvergenceEffect(spellContext);
    if (firstEffect instanceof EffectExplosion) {
      ExplosionConvergenceHelper.handleExplosionConvergence(serverLevel, pos, shooter, spellStats, spellContext,
          this);
    } else if (firstEffect instanceof EffectConjureWater) {
      WaterConvergenceHelper.handleWaterConvergence(serverLevel, pos, shooter, spellContext, this);
    } else if (firstEffect instanceof EffectConjureTerrain) {
      ConjureTerrainConvergenceHelper.handleConjureTerrain(serverLevel, pos, shooter, spellContext, this);
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

    ItemStack casterTool = spellContext.getCasterTool();
    MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
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

  @Nullable
  private AbstractEffect findFirstConvergenceEffect(SpellContext context) {
    SpellContext iterator = context.clone();

    while (iterator.hasNextPart()) {
      AbstractSpellPart next = iterator.nextPart();

      if (next instanceof AbstractEffect effect) {
        if (effect instanceof EffectExplosion) {
          return effect;
        } else if (effect instanceof EffectConjureWater) {
          return effect;
        } else if (effect instanceof EffectConjureTerrain) {
          return effect;
        }
      }
    }

    return null;
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
    return augmentSetOf(
        AugmentHollow.INSTANCE,
        AugmentSphere.INSTANCE,
        AugmentCube.INSTANCE,
        AugmentFlatten.INSTANCE);
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
        "Generates hollow shapes, placing only the outer shell. Only valid with Conjure Terrain.");
    map.put(AugmentSphere.INSTANCE,
        "Generates spherical shapes. When flattened, produces circles. Only valid with Conjure Terrain.");
    map.put(AugmentCube.INSTANCE,
        "Generates cube shapes (default). When flattened, produces squares. Only valid with Conjure Terrain.");
    map.put(AugmentFlatten.INSTANCE,
        "Projects 3D shapes into 2D based on the caster's look direction. Only valid with Conjure Terrain.");
  }

  public GeometryDescription resolveGeometryDescription(SpellContext context, @Nullable LivingEntity caster) {
    return ConvergenceCompatibilityHelper.resolveGeometryDescription(context, caster);
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
