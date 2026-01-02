package com.github.ars_zero.common.entity.explosion;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.common.config.ServerConfig;
import com.github.ars_zero.common.entity.AbstractConvergenceEntity;
import com.github.ars_zero.common.entity.IAnchorLerp;
import com.github.ars_zero.common.explosion.LargeExplosionDamage;
import com.github.ars_zero.common.explosion.LargeExplosionPrecompute;
import com.github.ars_zero.common.explosion.ExplosionWorkList;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import org.jetbrains.annotations.Nullable;

public class ExplosionControllerEntity extends AbstractConvergenceEntity implements IAnchorLerp {
    private enum AnimState {
        CHARGING,
        IDLE,
        EXPLODING
    }

    private static final EntityDataAccessor<Float> DATA_CHARGE = SynchedEntityData
            .defineId(ExplosionControllerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FIRE_POWER = SynchedEntityData
            .defineId(ExplosionControllerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_EXPLODING = SynchedEntityData
            .defineId(ExplosionControllerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData
            .defineId(ExplosionControllerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_ANIM_STATE = SynchedEntityData
            .defineId(ExplosionControllerEntity.class, EntityDataSerializers.INT);
    private static final double CHARGE_PER_TICK_DIVISOR = 80.0;
    private static final double LOW_CHARGE_THRESHOLD = 0.10;
    private static final double BASE_EXPLODE_ANIMATION_SECONDS = 1.0;
    private static final double BASE_RADIUS_FOR_TIMING = 5.0;
    private static final double TARGET_RADIUS_FOR_TIMING = 27.0;
    private static final double TARGET_EXPLODE_ANIMATION_SECONDS = 30.0;
    private static final double BASELINE_RADIUS_FOR_SPEED = 16.0;

    private boolean exploding;
    private int explodeAnimationStartTick;
    private int explodeAnimationDurationTicks;
    private double radius;
    private AnimState animState = AnimState.CHARGING;
    private float baseDamage;
    private float powerMultiplier;
    private float charge;
    private double firePower;
    private Vec3 explosionCenter;
    private double explosionRadius;

    private int aoeLevel;
    private int amplifyLevel;
    private int dampenLevel;

    @Nullable
    private AnimState lastClientAnimState = null;

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private Object idleSoundInstance = null;

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private Object chargeSoundInstance = null;

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private Object primingSoundInstance = null;

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private Object resolverSoundInstance = null;

    private ExplosionWorkList workList;
    private int nextWorkIndex;
    private int lastProcessedRing = -1;

    private long[] deferredPositions;
    private int deferredSize;

    private static final double CHARGING_ANIMATION_LENGTH = 4;
    private static final double IDLE_ANIMATION_LENGTH = 2.0;
    private static final double DEFAULT_CHARGE_TIME_SECONDS = 4.0;
    private static final double DEFAULT_IDLE_TIME_SECONDS = 2.0;

    private boolean activateSoundPlayed = false;

    @Nullable
    private SoundEvent warningSound = null;
    private boolean warningSoundPlayed = false;

    @Nullable
    private SoundEvent resolveSound = null;
    private boolean resolveSoundPlayed = false;

    public ExplosionControllerEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.charge = 0.0f;
        this.firePower = 0.0;
        this.explodeAnimationStartTick = 0;
        this.explodeAnimationDurationTicks = (int) (BASE_EXPLODE_ANIMATION_SECONDS * 20.0) - 2;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
    }

    public int getExplodeAnimationStartTick() {
        return this.explodeAnimationStartTick;
    }

    public int getExplodeAnimationDurationTicks() {
        return this.explodeAnimationDurationTicks;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<ExplosionControllerEntity> controller = new AnimationController<>(this, "controller", 0,
                this::predicate);
        controllers.add(controller);
    }

    private PlayState predicate(AnimationState<ExplosionControllerEntity> state) {
        AnimState anim = getAnimState();
        double chargeTimeSeconds = calculateChargeTimeSeconds();
        int remainingLifespan = this.getLifespan();
        int maxLifespan = this.getMaxLifespan();
        double lifespanSpeedMultiplier = calculateLifespanSpeedMultiplier(remainingLifespan, maxLifespan);

        switch (anim) {
            case EXPLODING -> {
                if (this.level().isClientSide && explodeAnimationStartTick == 0) {
                    explodeAnimationStartTick = this.tickCount;
                }
                double radius = getRadius();
                double animationSpeed = BASELINE_RADIUS_FOR_SPEED / Math.max(0.1, radius);
                state.getController().setAnimationSpeed(animationSpeed);
                return state.setAndContinue(RawAnimation.begin().thenPlay("explode"));
            }
            case IDLE -> {
                double idleAnimationSpeed = (IDLE_ANIMATION_LENGTH / chargeTimeSeconds) * lifespanSpeedMultiplier;
                idleAnimationSpeed = Math.max(1.0, idleAnimationSpeed);
                state.getController().setAnimationSpeed(idleAnimationSpeed);
                return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
            case CHARGING -> {
                double animationSpeed = (CHARGING_ANIMATION_LENGTH / chargeTimeSeconds) * lifespanSpeedMultiplier;
                state.getController().setAnimationSpeed(animationSpeed);
                return state.setAndContinue(RawAnimation.begin().thenPlay("charge"));
            }
        }

        return PlayState.STOP;
    }

    public double calculateLifespanSpeedMultiplier(int remainingLifespan, int maxLifespan) {
        if (maxLifespan <= 0) {
            return 1.0;
        }

        if (remainingLifespan <= 1) {
            return maxLifespan * 10.0;
        }

        double normalizedLifespan = (double) remainingLifespan / maxLifespan;
        double inverseNormalized = 1.0 / Math.max(0.01, normalizedLifespan);

        return Math.max(1.0, inverseNormalized);
    }

    float POWER_FACTOR = 32.0f;

    public double calculateChargeTimeSeconds() {
        double power = getFirePower();
        double chargePerTick = (1.0 + power / POWER_FACTOR) / CHARGE_PER_TICK_DIVISOR;
        double ticksToFullCharge = 1.0 / chargePerTick;
        double secondsToFullCharge = ticksToFullCharge / 20.0;
        return secondsToFullCharge;
    }

    public double calculateIdleTimeSeconds() {
        // Calculate idle time proportionally to charge time reduction
        // If charge time is reduced by X%, idle time is also reduced by X%
        double actualChargeTime = calculateChargeTimeSeconds();
        double ratio = actualChargeTime / DEFAULT_CHARGE_TIME_SECONDS;
        return DEFAULT_IDLE_TIME_SECONDS * ratio;
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClientTick() {
        AnimState current = getAnimState();
        if (current != this.lastClientAnimState) {
            this.lastClientAnimState = current;
            AnimatableManager manager = this.getAnimatableInstanceCache().getManagerForId(this.getId());
            if (manager != null) {
                Object controllerObj = manager.getAnimationControllers().get("controller");
                if (controllerObj instanceof AnimationController<?> controller) {
                    controller.forceAnimationReset();
                }
            }
        }

        if (isExploding()) {
            if (this.idleSoundInstance != null) {
                Object[] ref = { this.idleSoundInstance };
                ExplosionSoundHelper.stopIdleSound(ref);
                this.idleSoundInstance = ref[0];
            }
            if (this.chargeSoundInstance != null) {
                Object[] ref = { this.chargeSoundInstance };
                ExplosionSoundHelper.stopChargeSound(ref);
                this.chargeSoundInstance = ref[0];
            }
            if (this.primingSoundInstance != null) {
                Object[] ref = { this.primingSoundInstance };
                ExplosionSoundHelper.stopPrimingSound(ref);
                this.primingSoundInstance = ref[0];
            }
            if (this.resolverSoundInstance != null) {
                Object[] ref = { this.resolverSoundInstance };
                ExplosionSoundHelper.stopResolverSound(ref);
                this.resolverSoundInstance = ref[0];
            }
            if (explodeAnimationStartTick > 0) {
                int ticksSinceExplode = this.tickCount - explodeAnimationStartTick;
                int durationTicks = ExplosionProcessHelper.calculateExplodeAnimationDurationTicks(getRadius(),
                        BASE_RADIUS_FOR_TIMING, TARGET_RADIUS_FOR_TIMING, BASE_EXPLODE_ANIMATION_SECONDS,
                        TARGET_EXPLODE_ANIMATION_SECONDS);
                if (ticksSinceExplode >= durationTicks) {
                    this.discard();
                }
            }
            return;
        }

        float charge = this.getCharge();
        int remainingLifespan = this.getLifespan();
        boolean shouldPlayIdle = (charge >= 1.0f || (remainingLifespan < 20 && charge < 1.0f));
        boolean shouldPlayCharge = (charge < 1.0f && remainingLifespan >= 20);
        boolean shouldPlayPriming = (remainingLifespan <= 19);

        if (shouldPlayCharge && this.chargeSoundInstance == null) {
            Object[] ref = { this.chargeSoundInstance };
            ExplosionSoundHelper.startChargeSound(this, ref);
            this.chargeSoundInstance = ref[0];
        } else if (!shouldPlayCharge && this.chargeSoundInstance != null) {
            Object[] ref = { this.chargeSoundInstance };
            ExplosionSoundHelper.stopChargeSound(ref);
            this.chargeSoundInstance = ref[0];
        }

        if (shouldPlayPriming && this.primingSoundInstance == null) {
            Object[] ref = { this.primingSoundInstance };
            ExplosionSoundHelper.startPrimingSound(this, ref);
            this.primingSoundInstance = ref[0];
        }

        if (!shouldPlayPriming && this.primingSoundInstance != null) {
            Object[] ref = { this.primingSoundInstance };
            ExplosionSoundHelper.stopPrimingSound(ref);
            this.primingSoundInstance = ref[0];
        }

        if (shouldPlayIdle && this.idleSoundInstance == null && !shouldPlayPriming) {
            Object[] ref = { this.idleSoundInstance };
            ExplosionSoundHelper.startIdleSound(this, ref);
            this.idleSoundInstance = ref[0];
        } else if (!shouldPlayIdle && this.idleSoundInstance != null && remainingLifespan > 19) {
            Object[] ref = { this.idleSoundInstance };
            ExplosionSoundHelper.stopIdleSound(ref);
            this.idleSoundInstance = ref[0];
        }

        if (remainingLifespan == 19 && this.resolverSoundInstance == null && this.resolveSound != null) {
            Object[] ref = { this.resolverSoundInstance };
            ExplosionSoundHelper.startResolverSound(this, ref);
            this.resolverSoundInstance = ref[0];
        }
    }

    public double getFirePower() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_FIRE_POWER);
        }
        return this.firePower;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGE, 0.0f);
        builder.define(DATA_FIRE_POWER, 0.0f);
        builder.define(DATA_EXPLODING, false);
        builder.define(DATA_RADIUS, 0.0f);
        builder.define(DATA_ANIM_STATE, AnimState.CHARGING.ordinal());
    }

    private AnimState getAnimState() {
        if (this.level().isClientSide) {
            int ordinal = this.entityData.get(DATA_ANIM_STATE);
            return AnimState.values()[Math.max(0, Math.min(AnimState.values().length - 1, ordinal))];
        }
        return this.animState;
    }

    public boolean isExploding() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_EXPLODING);
        }
        return this.exploding;
    }

    public float getCharge() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_CHARGE);
        }
        return this.charge;
    }

    public double getRadius() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_RADIUS);
        }
        return this.radius;
    }

    @Override
    public double getLerpValue() {
        return 0.02;
    }

    @Override
    public double getMaxDelta() {
        return 0.1f;
    }

    private void setCharge(float newCharge) {
        this.charge = Math.max(0.0f, Math.min(1.0f, newCharge));
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_CHARGE, this.charge);
        }
    }

    public void setExplosionParams(double radius, float baseDamage, float powerMultiplier, int aoeLevel,
            int amplifyLevel, int dampenLevel) {
        this.radius = Math.max(0.0, radius);
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_RADIUS, (float) this.radius);
        }
        this.baseDamage = Math.max(0.0f, baseDamage);
        this.powerMultiplier = Math.max(0.0f, powerMultiplier);
        this.aoeLevel = aoeLevel;
        this.amplifyLevel = amplifyLevel;
        this.dampenLevel = dampenLevel;
    }

    public void setWarningSound(@Nullable SoundEvent sound) {
        this.warningSound = sound;
    }

    public void setResolveSound(@Nullable SoundEvent sound) {
        this.resolveSound = sound;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public SoundEvent getResolveSound() {
        return this.resolveSound;
    }

    @Override
    public void addLifespan(LivingEntity shooter, SpellStats spellStats, SpellContext spellContext,
            SpellResolver resolver) {
        super.addLifespan(shooter, spellStats, spellContext, resolver);

        if (shooter instanceof Player player) {
            AttributeInstance firePowerAttr = player.getAttribute(ModRegistry.FIRE_POWER);
            double power = 0;
            if (firePowerAttr != null) {
                power = firePowerAttr.getValue();
            }
            this.firePower = power;
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_FIRE_POWER, (float) power);
            }
            double chargePerTick = (1.0 + power / POWER_FACTOR) / CHARGE_PER_TICK_DIVISOR;
            float newCharge = (float) (this.charge + chargePerTick);
            setCharge(newCharge);

            if (!this.level().isClientSide && newCharge >= 1.0f && this.animState == AnimState.CHARGING) {
                this.animState = AnimState.IDLE;
                this.entityData.set(DATA_ANIM_STATE, AnimState.IDLE.ordinal());
            }
        }
    }

    @Override
    protected void onLifespanReached() {
        if (this.level().isClientSide) {
            return;
        }

        if (!exploding) {
            startExplosion();
        }
    }

    private void startExplosion() {
        this.exploding = true;
        this.animState = AnimState.EXPLODING;
        this.explodeAnimationStartTick = this.tickCount;
        this.lastProcessedRing = -1;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_EXPLODING, true);
            this.entityData.set(DATA_ANIM_STATE, AnimState.EXPLODING.ordinal());
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 center = this.position();
        float currentCharge = this.charge;

        double calculatedRadius = ExplosionProcessHelper.calculateRadius(currentCharge, this.firePower, this.aoeLevel,
                this.dampenLevel);

        if (currentCharge <= LOW_CHARGE_THRESHOLD) {
            createRegularExplosion(serverLevel, center, calculatedRadius);
            return;
        }

        if (!activateSoundPlayed) {
            ExplosionSoundHelper.playActivateSound(serverLevel, this.getX(), this.getY(), this.getZ(),
                    calculatedRadius);
            activateSoundPlayed = true;
        }

        // Spawn explosion fire projectiles
        ExplosionProcessHelper.spawnExplosionFireProjectiles(serverLevel, center, calculatedRadius, this.firePower,
                currentCharge);

        // Shake nearby players
        ExplosionShakeHelper.shakeNearbyPlayers(serverLevel, center, calculatedRadius);

        float adjustedDamage = ExplosionProcessHelper.calculateAdjustedDamage(this.baseDamage, this.amplifyLevel,
                this.dampenLevel, currentCharge, this.firePower);
        float adjustedPower = ExplosionProcessHelper.calculateAdjustedPower(this.powerMultiplier, currentCharge);
        this.explosionCenter = center;
        this.explosionRadius = calculatedRadius;

        LargeExplosionDamage.apply(serverLevel, this, center, calculatedRadius, adjustedDamage, adjustedPower);

        this.workList = LargeExplosionPrecompute.compute(this.level(), this.blockPosition(), calculatedRadius);
        this.nextWorkIndex = 0;

        if (!this.level().isClientSide) {
            this.entityData.set(DATA_RADIUS, (float) calculatedRadius);
        }

        this.explodeAnimationDurationTicks = ExplosionProcessHelper.calculateExplodeAnimationDurationTicks(
                calculatedRadius, BASE_RADIUS_FOR_TIMING, TARGET_RADIUS_FOR_TIMING, BASE_EXPLODE_ANIMATION_SECONDS,
                TARGET_EXPLODE_ANIMATION_SECONDS);

        if (workList == null || workList.size() == 0) {
            serverLevel.explode(this, center.x, center.y, center.z, (float) Math.max(0.5, calculatedRadius),
                    Level.ExplosionInteraction.NONE);
        }
    }

    private void createRegularExplosion(ServerLevel serverLevel, Vec3 center, double radius) {
        serverLevel.explode(this, center.x, center.y, center.z, (float) radius, Level.ExplosionInteraction.BLOCK);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            handleClientTick();
            return;
        }

        if (!exploding && this.level() instanceof ServerLevel serverLevel) {
            float charge = this.getCharge();
            int remainingLifespan = this.getLifespan();

            if (remainingLifespan == 19 && !resolveSoundPlayed && resolveSound != null) {
                resolveSoundPlayed = true;
            }

            if (remainingLifespan <= 19 && charge > LOW_CHARGE_THRESHOLD && !warningSoundPlayed
                    && warningSound != null) {
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), warningSound, SoundSource.NEUTRAL,
                        1.0f, 1.0f);
                warningSoundPlayed = true;
            }

            if (this.tickCount % 2 == 0) {
                ExplosionParticleHelper.spawnSpiralParticles(serverLevel, this.position(), charge, this.tickCount,
                        this.firePower);
            }
        }

        if (!exploding) {
            return;
        }

        int ticksSinceExplode = this.tickCount - explodeAnimationStartTick;
        boolean canDiscard = ticksSinceExplode >= this.explodeAnimationDurationTicks;

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            if (canDiscard) {
                this.discard();
            }
            return;
        }
        if (workList == null) {
            if (canDiscard) {
                this.discard();
            }
            return;
        }

        int maxPerTick = Math.max(1, ServerConfig.LARGE_EXPLOSION_MAX_BLOCKS_PER_TICK.get());
        ExplosionProcessHelper.ProcessResult result = ExplosionProcessHelper.processWorkList(
                serverLevel, this, workList, nextWorkIndex, this.explosionCenter, this.explosionRadius,
                this.deferredPositions, this.deferredSize, maxPerTick, this.firePower, this.amplifyLevel);

        if (result.highestRing > 14 && result.highestRing > this.lastProcessedRing) {
            for (int ring = this.lastProcessedRing + 1; ring <= result.highestRing; ring++) {
                if (ring > 14) {
                    ExplosionSoundHelper.playRingExplodeSound(serverLevel, this.explosionCenter.x,
                            this.explosionCenter.y,
                            this.explosionCenter.z);
                }
            }
            this.lastProcessedRing = result.highestRing;
        } else if (result.highestRing > this.lastProcessedRing) {
            this.lastProcessedRing = result.highestRing;
        }

        this.nextWorkIndex = result.nextWorkIndex;
        this.deferredSize = result.deferredSize;
        this.deferredPositions = result.deferredPositions;

        if (result.shouldDiscard) {
            if (this.deferredSize > 0) {
                this.workList = ExplosionProcessHelper.rollDeferredIntoWork(this.deferredPositions, this.deferredSize);
                this.nextWorkIndex = 0;
                this.deferredSize = 0;
                return;
            }
            if (canDiscard) {
                this.discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("exploding")) {
            this.exploding = compound.getBoolean("exploding");
        }
        if (compound.contains("radius")) {
            this.radius = compound.getDouble("radius");
        }
        if (compound.contains("base_damage")) {
            this.baseDamage = compound.getFloat("base_damage");
        }
        if (compound.contains("power_multiplier")) {
            this.powerMultiplier = compound.getFloat("power_multiplier");
        }
        if (compound.contains("charge")) {
            setCharge(compound.getFloat("charge"));
        }
        if (compound.contains("fire_power")) {
            this.firePower = compound.getDouble("fire_power");
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_FIRE_POWER, (float) this.firePower);
            }
        }
        if (compound.contains("aoe_level")) {
            this.aoeLevel = compound.getInt("aoe_level");
        }
        if (compound.contains("amplify_level")) {
            this.amplifyLevel = compound.getInt("amplify_level");
        }
        if (compound.contains("dampen_level")) {
            this.dampenLevel = compound.getInt("dampen_level");
        }
        if (compound.contains("explodeAnimationStartTick")) {
            this.explodeAnimationStartTick = compound.getInt("explodeAnimationStartTick");
        }
        if (compound.contains("explodeAnimationDurationTicks")) {
            this.explodeAnimationDurationTicks = compound.getInt("explodeAnimationDurationTicks");
        } else {
            this.explodeAnimationDurationTicks = (int) (BASE_EXPLODE_ANIMATION_SECONDS * 20.0);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("exploding", this.exploding);
        compound.putDouble("radius", this.radius);
        compound.putFloat("base_damage", this.baseDamage);
        compound.putFloat("power_multiplier", this.powerMultiplier);
        compound.putFloat("charge", this.charge);
        compound.putDouble("fire_power", this.firePower);
        compound.putInt("aoe_level", this.aoeLevel);
        compound.putInt("amplify_level", this.amplifyLevel);
        compound.putInt("dampen_level", this.dampenLevel);
        compound.putInt("explodeAnimationStartTick", this.explodeAnimationStartTick);
        compound.putInt("explodeAnimationDurationTicks", this.explodeAnimationDurationTicks);
    }

}
