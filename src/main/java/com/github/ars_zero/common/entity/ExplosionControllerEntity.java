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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ExplosionControllerEntity extends AbstractConvergenceEntity {
    private static final int SILENT_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final EntityDataAccessor<Float> DATA_CHARGE = SynchedEntityData.defineId(ExplosionControllerEntity.class, EntityDataSerializers.FLOAT);
    private static final double CHARGE_PER_TICK_DIVISOR = 80.0;
    private static final double LOW_CHARGE_THRESHOLD = 0.10;

    private boolean active;
    private double radius;
    private float baseDamage;
    private float powerMultiplier;
    private float charge;
    private double firePower; // Store firepower for radius calculation

    private ExplosionWorkList workList;
    private int nextWorkIndex;

    private long[] deferredPositions;
    private int deferredSize;

    public ExplosionControllerEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.charge = 0.0f;
        this.firePower = 0.0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGE, 0.0f);
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

    public void setExplosionParams(double radius, float baseDamage, float powerMultiplier) {
        this.radius = Math.max(0.0, radius);
        this.baseDamage = Math.max(0.0f, baseDamage);
        this.powerMultiplier = Math.max(0.0f, powerMultiplier);
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
            double chargePerTick = (1.0 + power) / CHARGE_PER_TICK_DIVISOR;
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

        // Calculate radius based on charge and firepower: Radius = Charge * 10 + Charge * (1 + FIREPOWER)
        // Simplified: Radius = Charge * (11 + FIREPOWER)
        double calculatedRadius = currentCharge * (11.0 + this.firePower);
        
        ArsZero.LOGGER.info("[ExplosionControllerEntity] Starting explosion - charge: {}, firePower: {}, calculatedRadius: {}, baseDamage: {}, powerMultiplier: {}", 
            currentCharge, this.firePower, calculatedRadius, this.baseDamage, this.powerMultiplier);

        if (currentCharge <= LOW_CHARGE_THRESHOLD) {
            ArsZero.LOGGER.info("[ExplosionControllerEntity] Low charge, creating regular explosion");
            createRegularExplosion(serverLevel, center, calculatedRadius);
            this.discard();
            return;
        }

        double adjustedRadius = calculatedRadius;
        float adjustedDamage = this.baseDamage * currentCharge;
        float adjustedPower = this.powerMultiplier * currentCharge;

        ArsZero.LOGGER.info("[ExplosionControllerEntity] Large explosion - adjustedRadius: {}, adjustedDamage: {}, adjustedPower: {}", 
            adjustedRadius, adjustedDamage, adjustedPower);

        LargeExplosionDamage.apply(serverLevel, this, center, adjustedRadius, adjustedDamage, adjustedPower);

        this.workList = LargeExplosionPrecompute.compute(this.level(), this.blockPosition(), adjustedRadius);
        this.nextWorkIndex = 0;

        ArsZero.LOGGER.info("[ExplosionControllerEntity] WorkList computed - size: {}", workList != null ? workList.size() : 0);

        // Even if workList is empty, we should still create a visual explosion effect
        // The damage has already been applied above, so we can discard after a brief delay
        if (workList == null || workList.size() == 0) {
            // Create a small visual explosion effect even if no blocks to destroy
            serverLevel.explode(this, center.x, center.y, center.z, (float) Math.max(0.5, adjustedRadius), Level.ExplosionInteraction.NONE);
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

            boolean removed = serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), SILENT_UPDATE_FLAGS);
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
}

