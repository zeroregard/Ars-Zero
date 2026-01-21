package com.github.ars_zero.common.glyph.convergence;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.spell.IMultiPhaseCaster;
import com.github.ars_zero.common.spell.ISubsequentEffectProvider;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.MultiPhaseCastContextRegistry;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellPhase;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.WrappedSpellResolver;
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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
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
                    resolver, this);
        } else if (hasEffect(spellContext, EffectConjureWater.class)) {
            WaterConvergenceHelper.handleWaterConvergence(serverLevel, pos, shooter, spellContext, resolver, this);
        } else if (rayTraceResult instanceof EntityHitResult entityHitResult) {
            ChargerHelper.handlePlayerCharger(serverLevel, pos, entityHitResult, shooter, spellContext, resolver, this);
        } else if (rayTraceResult instanceof BlockHitResult blockHitResult) {
            ChargerHelper.handleBlockCharger(serverLevel, pos, blockHitResult, shooter, spellContext, resolver, this);
        }
    }

    void updateTemporalContext(LivingEntity shooter, Entity entity, SpellContext spellContext, SpellResolver resolver) {
        
        IMultiPhaseCaster caster = IMultiPhaseCaster.from(spellContext, shooter);
        
        if (caster == null && spellContext.tag.contains("ars_zero:turret_pos") && spellContext.level instanceof ServerLevel serverLevel) {
            long posLong = spellContext.tag.getLong("ars_zero:turret_pos");
            BlockPos turretPos = BlockPos.of(posLong);
            BlockEntity tile = serverLevel.getBlockEntity(turretPos);
            if (tile instanceof IMultiPhaseCaster multiPhaseCaster) {
                caster = multiPhaseCaster;
            }
        }
        if (caster == null) {
            return;
        }
        
        MultiPhaseCastContext context = caster.getCastContext();
        if (context == null) {
            ArsZero.LOGGER.warn("[EffectConvergence] updateTemporalContext: No MultiPhaseCastContext found - caster={}, shooter={}", 
                spellContext.getCaster() != null ? spellContext.getCaster().getClass().getSimpleName() : "null",
                shooter != null ? shooter.getClass().getSimpleName() : "null");
            return;
        }

        SpellPhase phase = WrappedSpellResolver.extractPhase(resolver, context);
        if (phase == null) {
            if (context.beginResults.isEmpty() && !context.beginFinished && context.currentPhase == SpellPhase.TICK) {
                phase = SpellPhase.BEGIN;
            } else {
                phase = context.currentPhase;
            }
        }

        SpellResult entityResult = SpellResult.fromHitResultWithCaster(
                new EntityHitResult(entity),
                SpellEffectType.RESOLVED,
                spellContext.getCaster());

        switch (phase) {
            case BEGIN -> {
                context.beginResults.clear();
                context.beginResults.add(entityResult);
            }
            case TICK -> {
                context.tickResults.add(entityResult);
            }
            case END -> context.endResults.add(entityResult);
        }
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
        return augmentSetOf();
    }

    @Override
    public String getBookDescription() {
        return "Creates a convergence point that can be augmented with other effects. Combine with Explosion for a mega-explosion, or Conjure Water to fill an area.";
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
