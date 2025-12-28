package com.github.ars_zero.common.entity;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.config.ServerConfig;
import com.github.ars_zero.common.explosion.LargeExplosionDamage;
import com.github.ars_zero.common.explosion.LargeExplosionPrecompute;
import com.github.ars_zero.common.explosion.ExplosionWorkList;
import com.github.ars_zero.common.util.BlockImmutabilityUtil;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class ExplosionControllerEntity extends AbstractConvergenceEntity {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS;
    private static final EntityDataAccessor<Float> DATA_CHARGE = SynchedEntityData.defineId(ExplosionControllerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FIRE_POWER = SynchedEntityData.defineId(ExplosionControllerEntity.class, EntityDataSerializers.FLOAT);
    private static final double CHARGE_PER_TICK_DIVISOR = 160.0;
    private static final double LOW_CHARGE_THRESHOLD = 0.10;

    private boolean active;
    private double radius;
    private float baseDamage;
    private float powerMultiplier;
    private float charge;
    private double firePower; // Store firepower for radius calculation
    private Vec3 explosionCenter; // Store explosion center for distance calculations
    private double explosionRadius; // Store explosion radius for distance calculations
    
    private int aoeLevel;
    private int amplifyLevel;
    private int dampenLevel;

    private ExplosionWorkList workList;
    private int nextWorkIndex;

    private long[] deferredPositions;
    private int deferredSize;

    private static final double CHARGING_ANIMATION_LENGTH = 4.0417;
    private static final double IDLE_ANIMATION_LENGTH = 2.0;

    public ExplosionControllerEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.charge = 0.0f;
        this.firePower = 0.0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<ExplosionControllerEntity> state) {
        float charge = this.getCharge();
        
        if (charge >= 1.0f) {
            if (this.level().getGameTime() % 20 == 0) {
                ArsZero.LOGGER.debug("ExplosionControllerEntity: Switching to idle animation, charge={}", charge);
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        } else {
            double chargeTimeSeconds = calculateChargeTimeSeconds();
            double animationSpeed = CHARGING_ANIMATION_LENGTH / chargeTimeSeconds;
            
            if (this.level().getGameTime() % 20 == 0) {
                ArsZero.LOGGER.debug("ExplosionControllerEntity animation: charge={}, firePower={}, chargeTimeSeconds={}, animationSpeed={}, expectedTicksToFull={}", 
                    charge, getFirePower(), chargeTimeSeconds, animationSpeed, chargeTimeSeconds * 20.0);
            }
            
            state.getController().setAnimationSpeed(animationSpeed);
            return state.setAndContinue(RawAnimation.begin().thenPlay("charge"));
        }
    }

    private double calculateChargeTimeSeconds() {
        double power = getFirePower();
        double chargePerTick = (1.0 + power / 8.0) / CHARGE_PER_TICK_DIVISOR;
        double ticksToFullCharge = 1.0 / chargePerTick;
        double secondsToFullCharge = ticksToFullCharge / 20.0;
        return secondsToFullCharge;
    }

    private double getFirePower() {
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
    }

    public float getCharge() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_CHARGE);
        }
        return this.charge;
    }

    private void setCharge(float newCharge) {
        this.charge = Math.max(0.0f, Math.min(1.0f, newCharge));
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_CHARGE, this.charge);
        }
    }

    public void setExplosionParams(double radius, float baseDamage, float powerMultiplier, int aoeLevel, int amplifyLevel, int dampenLevel) {
        this.radius = Math.max(0.0, radius);
        this.baseDamage = Math.max(0.0f, baseDamage);
        this.powerMultiplier = Math.max(0.0f, powerMultiplier);
        this.aoeLevel = aoeLevel;
        this.amplifyLevel = amplifyLevel;
        this.dampenLevel = dampenLevel;
    }

    @Override
    public void addLifespan(LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        super.addLifespan(shooter, spellStats, spellContext, resolver);
        
        if (shooter instanceof Player player) {
            AttributeInstance firePowerAttr = player.getAttribute(ModRegistry.FIRE_POWER);
            double power = 0;
            if(firePowerAttr != null) {
                power = firePowerAttr.getValue();
            }
            // Store the latest firepower value for radius calculation
            this.firePower = power;
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_FIRE_POWER, (float) power);
            }
            double chargePerTick = (1.0 + power / 8.0d) / CHARGE_PER_TICK_DIVISOR;
            float newCharge = (float) (this.charge + chargePerTick);
            setCharge(newCharge);
        }
    }

    @Override
    protected void onLifespanReached() {
        if (this.level().isClientSide) {
            return;
        }

        if (!active) {
            startExplosion();
        }
    }

    private void startExplosion() {
        this.active = true;

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 center = this.position();
        float currentCharge = this.charge;

        double calculatedRadius = currentCharge * (14.0 + 3.0 * this.firePower);
        
        // Apply AOE: +1 radius per AOE level
        calculatedRadius += this.aoeLevel;
        
        // Apply Dampen: reduce radius by 0.5 per dampen level
        calculatedRadius -= 0.5 * this.dampenLevel;
        calculatedRadius = Math.max(0.0, calculatedRadius);

        if (currentCharge <= LOW_CHARGE_THRESHOLD) {
            createRegularExplosion(serverLevel, center, calculatedRadius);
            this.discard();
            return;
        }

        // Apply Amplify: +1 damage per amplify level
        float adjustedDamage = (this.baseDamage + this.amplifyLevel) * currentCharge;
        // Apply Dampen: reduce damage by 0.5 per dampen level
        adjustedDamage -= 0.5f * this.dampenLevel * currentCharge;
        adjustedDamage = Math.max(0.0f, adjustedDamage);
        
        float adjustedPower = this.powerMultiplier * currentCharge;
        this.explosionCenter = center;
        this.explosionRadius = calculatedRadius;

        LargeExplosionDamage.apply(serverLevel, this, center, calculatedRadius, adjustedDamage, adjustedPower);

        this.workList = LargeExplosionPrecompute.compute(this.level(), this.blockPosition(), calculatedRadius);
        this.nextWorkIndex = 0;

        // Even if workList is empty, we should still create a visual explosion effect
        // The damage has already been applied above, so we can discard after a brief delay
        if (workList == null || workList.size() == 0) {
            // Create a small visual explosion effect even if no blocks to destroy
            serverLevel.explode(this, center.x, center.y, center.z, (float) Math.max(0.5, calculatedRadius), Level.ExplosionInteraction.NONE);
            this.discard();
        }
    }

    private void createRegularExplosion(ServerLevel serverLevel, Vec3 center, double radius) {
        serverLevel.explode(this, center.x, center.y, center.z, (float) radius, Level.ExplosionInteraction.BLOCK);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide || !active) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (workList == null) {
            this.discard();
            return;
        }

        int maxPerTick = Math.max(1, ServerConfig.LARGE_EXPLOSION_MAX_BLOCKS_PER_TICK.get());
        int remaining = workList.size() - nextWorkIndex;
        int budget = Math.min(maxPerTick, remaining);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < budget; i++) {
            long packedPos = workList.positionAt(nextWorkIndex);
            int distSq = workList.distanceSquaredAt(nextWorkIndex);
            nextWorkIndex++;

            pos.set(packedPos);
            if (serverLevel.isOutsideBuildHeight(pos)) {
                continue;
            }

            BlockState state = serverLevel.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (BlockImmutabilityUtil.isBlockImmutable(state)) {
                continue;
            }
            if (state.getDestroySpeed(serverLevel, pos) < 0.0f) {
                continue;
            }

            // Calculate destruction chance based on distance and block hardness
            if (!shouldDestroyBlock(serverLevel, pos, state, distSq)) {
                continue;
            }

            // Drop chance based on block hardness: hardness / 10, capped at 100%
            // Softer blocks (dirt, hardness 0.5) = 5% chance
            // Harder blocks (obsidian, hardness 50) = 100% chance
            float hardness = state.getDestroySpeed(serverLevel, pos);
            double dropChance = Math.min(1.0, hardness / 10.0);
            if (serverLevel.getRandom().nextDouble() < dropChance) {
                Block.dropResources(state, serverLevel, pos, null, this, ItemStack.EMPTY);
            }
            
            boolean removed = serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
            if (!removed) {
                defer(packedPos);
            }
        }

        if (nextWorkIndex >= workList.size()) {
            if (deferredSize > 0) {
                rollDeferredIntoWork();
                return;
            }
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("active")) {
            this.active = compound.getBoolean("active");
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("active", this.active);
        compound.putDouble("radius", this.radius);
        compound.putFloat("base_damage", this.baseDamage);
        compound.putFloat("power_multiplier", this.powerMultiplier);
        compound.putFloat("charge", this.charge);
        compound.putDouble("fire_power", this.firePower);
        compound.putInt("aoe_level", this.aoeLevel);
        compound.putInt("amplify_level", this.amplifyLevel);
        compound.putInt("dampen_level", this.dampenLevel);
    }

    private void defer(long packedPos) {
        if (deferredPositions == null) {
            deferredPositions = new long[1024];
        }
        if (deferredSize >= deferredPositions.length) {
            long[] next = new long[deferredPositions.length + (deferredPositions.length >> 1)];
            System.arraycopy(deferredPositions, 0, next, 0, deferredSize);
            deferredPositions = next;
        }
        deferredPositions[deferredSize] = packedPos;
        deferredSize++;
    }

    private void rollDeferredIntoWork() {
        ExplosionWorkList list = new ExplosionWorkList(deferredSize);
        for (int i = 0; i < deferredSize; i++) {
            list.add(deferredPositions[i], 0);
        }
        this.workList = list;
        this.nextWorkIndex = 0;
        this.deferredSize = 0;
    }

    /**
     * Determines if a block should be destroyed based on distance from explosion center and block hardness.
     * Closer blocks have higher chance, harder blocks have lower chance.
     */
    private boolean shouldDestroyBlock(ServerLevel level, BlockPos pos, BlockState state, int distanceSquared) {
        if (this.explosionCenter == null || this.explosionRadius <= 0) {
            return true;
        }

        double distance = Math.sqrt(distanceSquared);
        double normalizedDistance = Math.min(1.0, distance / this.explosionRadius);
        float hardness = state.getDestroySpeed(level, pos);
        
        if (hardness <= 3.5f) {
            return calculateSoftBlockChance(level, normalizedDistance, hardness);
        }
        
        return calculateHardBlockChance(level, normalizedDistance, hardness);
    }
    
    private boolean calculateSoftBlockChance(ServerLevel level, double normalizedDistance, float hardness) {
        if (normalizedDistance <= 0.75) {
            return true;
        }
        
        double edgeDistance = (normalizedDistance - 0.75) / 0.25;
        double minChanceAtEdge = 0.70 - ((hardness - 1.5f) / 2.0f) * 0.30;
        double finalChance = 1.0 - (edgeDistance * (1.0 - minChanceAtEdge));
        return level.getRandom().nextDouble() < finalChance;
    }
    
    private boolean calculateHardBlockChance(ServerLevel level, double normalizedDistance, float hardness) {
        double distanceFactor = 1.0 - normalizedDistance;
        double hardnessResistance = Math.min(0.80, hardness / 200.0);
        double baseChance = distanceFactor * (1.0 - hardnessResistance);
        
        double minChance = getMinimumChance(hardness);
        double finalChance = Math.max(minChance, baseChance);
        
        return level.getRandom().nextDouble() < finalChance;
    }
    
    private double getMinimumChance(float hardness) {
        if (hardness <= 5.0f) {
            return 0.50;
        } else if (hardness <= 10.0f) {
            return 0.30;
        }
        return 0.0;
    }
}

