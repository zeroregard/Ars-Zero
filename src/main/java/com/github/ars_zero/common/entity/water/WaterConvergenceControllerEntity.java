package com.github.ars_zero.common.entity.water;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.common.entity.AbstractConvergenceEntity;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class WaterConvergenceControllerEntity extends AbstractConvergenceEntity {

    private static final EntityDataAccessor<Integer> DATA_RADIUS = SynchedEntityData
            .defineId(WaterConvergenceControllerEntity.class, EntityDataSerializers.INT);
    private static final int DEFAULT_RADIUS = 8;

    private boolean started;
    private int nextIndex;
    private List<BlockPos> pattern;
    private float waterPower;
    private UUID casterUuid;

    public WaterConvergenceControllerEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public void setRadius(int radius) {
        int clamped = Math.max(1, radius);
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_RADIUS, clamped);
        }
    }

    public int getRadius() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_RADIUS);
        }
        return this.entityData.get(DATA_RADIUS);
    }

    public BlockPos getSphereCenterBlockPos() {
        int radius = getRadius();
        return this.blockPosition().above(radius);
    }

    public void setCaster(@NotNull LivingEntity caster) {
        this.casterUuid = caster.getUUID();
        this.waterPower = getWaterPower(caster);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RADIUS, DEFAULT_RADIUS);
    }

    @Override
    protected void onLifespanReached() {
        if (this.level().isClientSide) {
            return;
        }
        if (this.started) {
            return;
        }
        this.started = true;
        ensurePattern();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        if (!this.started) {
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ensurePattern();
        if (this.pattern == null || this.nextIndex >= this.pattern.size()) {
            this.discard();
            return;
        }

        int placedThisTick = 0;
        int maxPerTick = getMaxPlacementsPerTick(this.waterPower);

        while (placedThisTick < maxPerTick && this.nextIndex < this.pattern.size()) {
            BlockPos target = this.pattern.get(this.nextIndex);
            this.nextIndex++;

            if (!serverLevel.isLoaded(target)) {
                continue;
            }

            BlockState current = serverLevel.getBlockState(target);
            if (!shouldReplaceBlock(serverLevel, target, current, this.waterPower)) {
                continue;
            }

            serverLevel.setBlock(target, Blocks.WATER.defaultBlockState(), 3);
            placedThisTick++;
        }

        if (this.nextIndex >= this.pattern.size()) {
            this.discard();
        }
    }

    private void ensurePattern() {
        if (this.pattern != null) {
            return;
        }
        BlockPos sphereCenter = getSphereCenterBlockPos();
        this.pattern = WaterConvergencePattern.hemisphereBottomUp(sphereCenter, getRadius());
        this.nextIndex = Math.max(0, this.nextIndex);
    }

    protected boolean shouldReplaceBlock(ServerLevel level, BlockPos pos, BlockState current, float waterPower) {
        return current.isAir();
    }

    protected int getMaxPlacementsPerTick(float waterPower) {
        return 1;
    }

    private float getWaterPower(LivingEntity caster) {
        if (caster instanceof Player player) {
            AttributeInstance instance = player.getAttribute(ModRegistry.WATER_POWER);
            if (instance != null) {
                return (float) instance.getValue();
            }
        }
        return 0.0f;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("started")) {
            this.started = compound.getBoolean("started");
        }
        if (compound.contains("next_index")) {
            this.nextIndex = compound.getInt("next_index");
        }
        if (compound.contains("water_power")) {
            this.waterPower = compound.getFloat("water_power");
        }
        if (compound.contains("caster_uuid")) {
            this.casterUuid = compound.getUUID("caster_uuid");
        }
        if (compound.contains("radius")) {
            int radius = compound.getInt("radius");
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_RADIUS, Math.max(1, radius));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("started", this.started);
        compound.putInt("next_index", this.nextIndex);
        compound.putFloat("water_power", this.waterPower);
        if (this.casterUuid != null) {
            compound.putUUID("caster_uuid", this.casterUuid);
        }
        compound.putInt("radius", getRadius());
    }
}

