package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.EffectBeamEntity;
import com.github.ars_zero.common.spell.IMultiPhaseCaster;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.util.MathHelper;
import com.github.ars_zero.registry.ModParticleTimelines;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import com.hollingsworth.arsnouveau.common.block.BasicSpellTurret;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
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

    private record TurretAim(BlockPos turretPos, Direction facing) {}

    public static final String ID = "effect_beam";
    public static final EffectBeam INSTANCE = new EffectBeam();

    public ModConfigSpec.DoubleValue BEAM_RESOLVER_MANA_COST_MULTIPLIER;
    public ModConfigSpec.DoubleValue BEAM_BASE_DAMAGE;
    public ModConfigSpec.DoubleValue BEAM_AMPLIFY_DAMAGE_BONUS;

    public EffectBeam() {
        super(ArsZero.prefix(ID), "Effect Beam");
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        super.buildConfig(builder);
        builder.comment("Effect Beam Settings").push("effect_beam");
        BEAM_RESOLVER_MANA_COST_MULTIPLIER = builder.comment(
            "Mana cost multiplier for beam resolver. Each resolve (on hit) costs this percentage of the forwarded spell's total mana cost. Default is 0.1 (10%).")
            .defineInRange("beamResolverManaCostMultiplier", 0.1, 0.0, 1.0);
        BEAM_BASE_DAMAGE = builder.comment("Base magic damage dealt by the beam per tick. Default is 0.5.")
            .defineInRange("beamBaseDamage", 0.5, 0.0, 100.0);
        BEAM_AMPLIFY_DAMAGE_BONUS = builder.comment("Additional damage per Amplify augment level. Default is 0.5.")
            .defineInRange("beamAmplifyDamageBonus", 0.5, 0.0, 100.0);
        builder.pop();
    }

    public double getResolverManaCostMultiplier() {
        if (BEAM_RESOLVER_MANA_COST_MULTIPLIER == null) {
            return 0.1;
        }
        return BEAM_RESOLVER_MANA_COST_MULTIPLIER.get();
    }

    public float getBaseDamage() {
        if (BEAM_BASE_DAMAGE == null) {
            return 0.5f;
        }
        return BEAM_BASE_DAMAGE.get().floatValue();
    }

    public float getAmplifyDamageBonus() {
        if (BEAM_AMPLIFY_DAMAGE_BONUS == null) {
            return 0.5f;
        }
        return BEAM_AMPLIFY_DAMAGE_BONUS.get().floatValue();
    }

    private static TurretAim findTurretAim(ServerLevel level, BlockPos playerBlock) {
        BlockState atPlayer = level.getBlockState(playerBlock);
        if (atPlayer.getBlock() instanceof BasicSpellTurret) {
            return new TurretAim(playerBlock, atPlayer.getValue(BasicSpellTurret.FACING));
        }
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = playerBlock.relative(dir);
            BlockState state = level.getBlockState(neighbor);
            if (state.getBlock() instanceof BasicSpellTurret) {
                Direction facing = state.getValue(BasicSpellTurret.FACING);
                if (neighbor.relative(facing).equals(playerBlock)) {
                    return new TurretAim(neighbor, facing);
                }
            }
        }
        return null;
    }

    private static Vec3 getBeamAimTarget(Level world, LivingEntity shooter, SpellContext spellContext) {
        Vec3 eyePos;
        Vec3 lookVec;
        if (shooter instanceof FakePlayer && world instanceof ServerLevel serverLevel) {
            BlockPos shooterBlock = BlockPos.containing(shooter.position());
            TurretAim aim = findTurretAim(serverLevel, shooterBlock);
            if (aim != null) {
                var dispensePos = BasicSpellTurret.getDispensePosition(aim.turretPos, aim.facing);
                eyePos = new Vec3(dispensePos.x(), dispensePos.y(), dispensePos.z());
                lookVec = new Vec3(aim.facing.getStepX(), aim.facing.getStepY(), aim.facing.getStepZ()).normalize();
            } else {
                eyePos = shooter.getEyePosition(1.0f);
                lookVec = shooter.getLookAngle();
            }
        } else {
            eyePos = shooter.getEyePosition(1.0f);
            lookVec = shooter.getLookAngle();
        }
        Vec3 lookEnd = eyePos.add(lookVec.scale(EffectBeamEntity.RAY_LENGTH));
        Vec3 extendedEnd = lookEnd.add(lookVec.scale(0.5));
        BlockHitResult blockHit = world.clip(new net.minecraft.world.level.ClipContext(eyePos, extendedEnd, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, shooter));
        EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(shooter, eyePos, extendedEnd, shooter.getBoundingBox().inflate(EffectBeamEntity.RAY_LENGTH), e -> e.isPickable() && !e.isSpectator() && !(e instanceof EffectBeamEntity), EffectBeamEntity.RAY_LENGTH * EffectBeamEntity.RAY_LENGTH);
        if (entityHit != null) {
            double entityDist = eyePos.distanceTo(entityHit.getLocation());
            double blockDist = blockHit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : eyePos.distanceTo(blockHit.getLocation());
            return entityDist < blockDist ? entityHit.getLocation() : (blockHit.getType() == HitResult.Type.MISS ? lookEnd : blockHit.getLocation());
        }
        return blockHit.getType() == HitResult.Type.MISS ? lookEnd : blockHit.getLocation();
    }

    @Override
    public void onResolve(HitResult rayTraceResult, Level world, @Nullable LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide || !(world instanceof ServerLevel serverLevel) || shooter == null) {
            return;
        }

        Vec3 pos = safelyGetHitPos(rayTraceResult);
        Vec3 lookVec;
        BlockPos turretPos = null;
        Direction turretFacing = null;
        
        if (spellContext.getCaster().getCasterType() == SpellContext.CasterType.TURRET && spellContext.getCaster() instanceof com.hollingsworth.arsnouveau.api.spell.wrapped_caster.TileCaster tileCaster) {
            turretPos = tileCaster.getTile().getBlockPos();
            turretFacing = tileCaster.getFacingDirection();
            lookVec = new Vec3(turretFacing.getStepX(), turretFacing.getStepY(), turretFacing.getStepZ()).normalize();
        } else if (shooter instanceof FakePlayer && serverLevel != null) {
            BlockPos shooterBlock = BlockPos.containing(shooter.position());
            TurretAim aim = findTurretAim(serverLevel, shooterBlock);
            if (aim != null) {
                turretPos = aim.turretPos;
                turretFacing = aim.facing;
                lookVec = new Vec3(aim.facing.getStepX(), aim.facing.getStepY(), aim.facing.getStepZ()).normalize();
            } else {
                lookVec = shooter.getLookAngle();
            }
        } else {
            lookVec = shooter.getLookAngle();
        }
        
        float yaw = 0.0f;
        float pitch = 0.0f;
        if (lookVec.lengthSqr() >= 1.0E-12) {
            float[] yawPitch = MathHelper.vecToYawPitch(lookVec);
            yaw = yawPitch[0];
            pitch = yawPitch[1];
        }

        int lifetime = (int) (EffectBeamEntity.DEFAULT_LIFETIME_TICKS * spellStats.getDurationMultiplier());
        if (lifetime <= 0) {
            lifetime = EffectBeamEntity.DEFAULT_LIFETIME_TICKS;
        }

        float beamColorR = 1.0f;
        float beamColorG = 1.0f;
        float beamColorB = 1.0f;
        var timeline = spellContext.getParticleTimeline(ModParticleTimelines.BEAM_TIMELINE.get());
        if (timeline != null) {
            ParticleColor color = timeline.getColor();
            if (color != null) {
                beamColorR = color.getRed();
                beamColorG = color.getGreen();
                beamColorB = color.getBlue();
            }
        }

        boolean dampened = spellStats.getBuffCount(AugmentDampen.INSTANCE) > 0 || spellStats.getBuffCount(AugmentSensitive.INSTANCE) > 0;
        int amplifyLevel = spellStats.getBuffCount(AugmentAmplify.INSTANCE);
        float damage = getBaseDamage() + amplifyLevel * getAmplifyDamageBonus();
        int splitLevel = spellStats.getBuffCount(AugmentSplit.INSTANCE);
        if (splitLevel <= 0) {
            EffectBeamEntity beam = new EffectBeamEntity(serverLevel, pos.x, pos.y, pos.z, yaw, pitch, lifetime, beamColorR, beamColorG, beamColorB, shooter.getUUID(), dampened, damage);
            if (turretPos != null && turretFacing != null) {
                beam.setTurretInfo(turretPos, turretFacing);
            }
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
            EffectBeamEntity beam = new EffectBeamEntity(serverLevel, p.x, p.y, p.z, yaw, pitch, lifetime, beamColorR, beamColorG, beamColorB, shooter.getUUID(), dampened, damage);
            if (turretPos != null && turretFacing != null) {
                beam.setTurretInfo(turretPos, turretFacing);
            }
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
        return 50;
    }

    @Override
    protected void addDefaultAugmentLimits(Map<ResourceLocation, Integer> defaults) {
        defaults.put(AugmentSplit.INSTANCE.getRegistryName(), 3);
        defaults.put(AugmentSensitive.INSTANCE.getRegistryName(), 1);
        defaults.put(AugmentDampen.INSTANCE.getRegistryName(), 1);
        defaults.put(AugmentAmplify.INSTANCE.getRegistryName(), 10);
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentExtendTime.INSTANCE, AugmentSensitive.INSTANCE, AugmentSplit.INSTANCE, AugmentDampen.INSTANCE, AugmentAOE.INSTANCE, AugmentAmplify.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentExtendTime.INSTANCE, "Increases the beam duration");
        map.put(AugmentSensitive.INSTANCE, "Prevents the beam from dealing damage");
        map.put(AugmentSplit.INSTANCE, "Splits the beam into multiples");
        map.put(AugmentDampen.INSTANCE, "Stops the beam from hurting entities");
        map.put(AugmentAOE.INSTANCE, "Increases the radius of the circle when using Split");
        map.put(AugmentAmplify.INSTANCE, "Increases damage");
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
