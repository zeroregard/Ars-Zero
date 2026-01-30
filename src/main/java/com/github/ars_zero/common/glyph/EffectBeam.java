package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.EffectBeamEntity;
import com.github.ars_zero.common.spell.IMultiPhaseCaster;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.util.MathHelper;
import com.hollingsworth.arsnouveau.api.registry.ParticleTimelineRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EffectBeam extends AbstractEffect {

    public static final String ID = "effect_beam";
    public static final EffectBeam INSTANCE = new EffectBeam();

    public ModConfigSpec.DoubleValue BEAM_RESOLVER_MANA_COST_MULTIPLIER;

    public EffectBeam() {
        super(ArsZero.prefix(ID), "Effect Beam");
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        super.buildConfig(builder);
        builder.comment("Effect Beam Settings").push("effect_beam");
        BEAM_RESOLVER_MANA_COST_MULTIPLIER = builder.comment(
            "Mana cost multiplier for beam resolver. Each resolve (on hit) costs this percentage of the forwarded spell's total mana cost. Default is 0.05 (5%).")
            .defineInRange("beamResolverManaCostMultiplier", 0.05, 0.0, 1.0);
        builder.pop();
    }

    public double getResolverManaCostMultiplier() {
        if (BEAM_RESOLVER_MANA_COST_MULTIPLIER == null) {
            return 0.05;
        }
        return BEAM_RESOLVER_MANA_COST_MULTIPLIER.get();
    }

    @Override
    public void onResolve(HitResult rayTraceResult, Level world, @Nullable LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide || !(world instanceof ServerLevel serverLevel) || shooter == null) {
            return;
        }

        Vec3 pos = safelyGetHitPos(rayTraceResult);
        float yaw;
        float pitch;

        if (spellStats.hasBuff(AugmentSensitive.INSTANCE)) {
            IMultiPhaseCaster caster = IMultiPhaseCaster.from(spellContext, shooter);
            if (caster != null) {
                MultiPhaseCastContext castContext = caster.getCastContext();
                if (castContext != null && !castContext.beginResults.isEmpty()) {
                    SpellResult first = castContext.beginResults.get(0);
                    yaw = first.casterYaw;
                    pitch = first.casterPitch;
                } else {
                    yaw = shooter.getYRot();
                    pitch = shooter.getXRot();
                }
            } else {
                yaw = shooter.getYRot();
                pitch = shooter.getXRot();
            }
        } else {
            yaw = shooter.getYRot();
            pitch = shooter.getXRot();
        }

        int lifetime = (int) (EffectBeamEntity.DEFAULT_LIFETIME_TICKS * spellStats.getDurationMultiplier());
        if (lifetime <= 0) {
            lifetime = EffectBeamEntity.DEFAULT_LIFETIME_TICKS;
        }

        float beamColorR = 1.0f;
        float beamColorG = 1.0f;
        float beamColorB = 1.0f;
        var timeline = spellContext.getParticleTimeline(ParticleTimelineRegistry.PROJECTILE_TIMELINE.get());
        if (timeline != null) {
            ParticleColor color = timeline.getColor();
            if (color != null) {
                beamColorR = color.getRed();
                beamColorG = color.getGreen();
                beamColorB = color.getBlue();
            }
        }

        boolean dampened = spellStats.getBuffCount(AugmentDampen.INSTANCE) > 0;
        int splitLevel = spellStats.getBuffCount(AugmentSplit.INSTANCE);
        if (splitLevel <= 0) {
            EffectBeamEntity beam = new EffectBeamEntity(serverLevel, pos.x, pos.y, pos.z, yaw, pitch, lifetime, beamColorR, beamColorG, beamColorB, shooter.getUUID(), dampened);
            spellContext.setCanceled(true);
            SpellContext childContext = spellContext.makeChildContext();
            beam.setResolver(resolver.getNewResolver(childContext));
            serverLevel.addFreshEntity(beam);
            updateTemporalContext(shooter, beam, spellContext);
            return;
        }

        int maxSplitLevel = 3;
        int actualSplitLevel = Math.min(splitLevel, maxSplitLevel);
        int entityCount;
        double circleRadius;
        switch (actualSplitLevel) {
            case 1:
                entityCount = 3;
                circleRadius = 0.35;
                break;
            case 2:
                entityCount = 5;
                circleRadius = 0.55;
                break;
            case 3:
                entityCount = 7;
                circleRadius = 0.75;
                break;
            default:
                entityCount = 1;
                circleRadius = 0.0;
                break;
        }
        int aoeLevel = spellStats.getBuffCount(AugmentAOE.INSTANCE);
        circleRadius += aoeLevel * 0.4;

        spellContext.setCanceled(true);
        SpellContext childContext = spellContext.makeChildContext();
        SpellResolver childResolver = resolver.getNewResolver(childContext);
        Vec3 center = new Vec3(pos.x, pos.y, pos.z);
        Vec3 circleNormal = Vec3.directionFromRotation(pitch, yaw);
        List<Vec3> positions = MathHelper.getCirclePositions(center, circleNormal, circleRadius, entityCount);
        List<EffectBeamEntity> beams = new ArrayList<>();
        for (Vec3 p : positions) {
            EffectBeamEntity beam = new EffectBeamEntity(serverLevel, p.x, p.y, p.z, yaw, pitch, lifetime, beamColorR, beamColorG, beamColorB, shooter.getUUID(), dampened);
            beam.setResolver(childResolver);
            serverLevel.addFreshEntity(beam);
            beams.add(beam);
        }
        updateTemporalContextMultiple(shooter, beams, spellContext);
    }

    private void updateTemporalContext(LivingEntity shooter, EffectBeamEntity beam, SpellContext spellContext) {
        IMultiPhaseCaster caster = IMultiPhaseCaster.from(spellContext, shooter);
        if (caster == null) {
            return;
        }
        MultiPhaseCastContext context = caster.getCastContext();
        if (context == null) {
            return;
        }
        SpellResult beamResult = SpellResult.fromHitResultWithCaster(
                new EntityHitResult(beam),
                SpellEffectType.RESOLVED,
                spellContext.getCaster());
        context.beginResults.clear();
        context.beginResults.add(beamResult);
    }

    private void updateTemporalContextMultiple(LivingEntity shooter, List<EffectBeamEntity> beams, SpellContext spellContext) {
        IMultiPhaseCaster caster = IMultiPhaseCaster.from(spellContext, shooter);
        if (caster == null) {
            return;
        }
        MultiPhaseCastContext context = caster.getCastContext();
        if (context == null) {
            return;
        }
        if (!context.beginResults.isEmpty()) {
            for (SpellResult oldResult : context.beginResults) {
                if (oldResult.targetEntity instanceof EffectBeamEntity oldBeam && oldBeam.isAlive()) {
                    oldBeam.discard();
                }
            }
        }
        context.beginResults.clear();
        for (EffectBeamEntity beam : beams) {
            SpellResult beamResult = SpellResult.fromHitResultWithCaster(
                    new EntityHitResult(beam),
                    SpellEffectType.RESOLVED,
                    spellContext.getCaster());
            context.beginResults.add(beamResult);
        }
    }

    @Override
    public int getDefaultManaCost() {
        return 80;
    }

    @Override
    protected void addDefaultAugmentLimits(Map<ResourceLocation, Integer> defaults) {
        defaults.put(AugmentSplit.INSTANCE.getRegistryName(), 3);
        defaults.put(AugmentSensitive.INSTANCE.getRegistryName(), 1);
        defaults.put(AugmentDampen.INSTANCE.getRegistryName(), 1);
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentExtendTime.INSTANCE, AugmentSensitive.INSTANCE, AugmentSplit.INSTANCE, AugmentDampen.INSTANCE, AugmentAOE.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentExtendTime.INSTANCE, "Increases the beam duration");
        map.put(AugmentSensitive.INSTANCE, "Uses the look direction from when the spell was cast");
        map.put(AugmentSplit.INSTANCE, "Splits the beam into multiples");
        map.put(AugmentDampen.INSTANCE, "Stops the beam from hurting entities");
        map.put(AugmentAOE.INSTANCE, "Increases the radius of the circle when using Split");
    }

    @Override
    public String getBookDescription() {
        return "Creates a beam that persists for a short time, dealing magic damage and spawning particles along its path. The beam is oriented in your look direction. Add effects after the beam to resolve them on each hit; mana is drained per hit as a percentage of the cost of those subsequent effects (augments on the beam itself do not count toward this cost).";
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.THREE;
    }

    @NotNull
    @Override
    public Set<SpellSchool> getSchools() {
        return Set.of(SpellSchools.MANIPULATION);
    }
}
