package com.github.ars_zero.common.entity.water;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.common.entity.AbstractConvergenceEntity;
import com.github.ars_zero.common.util.BlockProtectionUtil;
import com.github.ars_zero.registry.ModSounds;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.List;
import java.util.UUID;

public class WaterConvergenceControllerEntity extends AbstractConvergenceEntity {

    private static final EntityDataAccessor<Integer> DATA_RADIUS = SynchedEntityData
            .defineId(WaterConvergenceControllerEntity.class, EntityDataSerializers.INT);
    private static final int DEFAULT_RADIUS = 20;
    private static final float PLACEMENT_MULTIPLIER = 4.0f;
    private static final int TICKS_PER_Y_LEVEL = 20;
    private static final double MANA_COST_PER_BLOCK = 0.2;
    private static final double FREE_MANA_AMOUNT = 64.0;

    private int nextIndex;
    private List<BlockPos> currentFloorPattern;
    private float waterPower;
    private UUID casterUuid;
    private int currentY;
    private int ticksOnCurrentY;
    private BlockPos centerPos;
    private double consumedMana;

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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RADIUS, DEFAULT_RADIUS);
    }

    @Override
    protected void onLifespanReached() {
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (getLifespan() > 0) {
            return;
        }

        ensureInitialized();
        if (this.centerPos == null) {
            this.discard();
            return;
        }

        if (this.currentY == Integer.MIN_VALUE) {
            int radius = getRadius();
            int startY = this.centerPos.getY() - radius;
            this.currentY = startY;
            this.ticksOnCurrentY = 0;
            computeCurrentFloor(serverLevel);
        }

        if (this.currentFloorPattern == null || this.currentFloorPattern.isEmpty()) {
            if (this.ticksOnCurrentY < TICKS_PER_Y_LEVEL) {
                this.ticksOnCurrentY++;
                return;
            } else {
                advanceToNextYLevel(serverLevel);
                this.ticksOnCurrentY = 0;
                if (this.currentY > this.centerPos.getY() + getRadius()) {
                    this.discard();
                    return;
                }
            }
        }

        boolean finishedCurrentY = (this.nextIndex >= this.currentFloorPattern.size());

        if (finishedCurrentY) {
            if (this.ticksOnCurrentY < TICKS_PER_Y_LEVEL) {
                this.ticksOnCurrentY++;
                return;
            } else {
                LivingEntity caster = serverLevel.getEntity(this.casterUuid) instanceof LivingEntity living ? living
                        : null;
                if (caster != null) {
                    double x = caster.getX();
                    double y = caster.getY();
                    double z = caster.getZ();
                    serverLevel.playSound(null, x, y, z, SoundEvents.AMBIENT_UNDERWATER_EXIT,
                            SoundSource.BLOCKS, 2.0f, 0.5f);
                }

                advanceToNextYLevel(serverLevel);
                this.ticksOnCurrentY = 0;
                if (this.currentY > this.centerPos.getY() + getRadius()) {
                    this.discard();
                    return;
                }
            }
        }

        int placedThisTick = 0;
        int maxPerTick = getMaxPlacementsPerTick(this.waterPower);

        LivingEntity caster = serverLevel.getEntity(this.casterUuid) instanceof LivingEntity living ? living : null;
        Player claimActor = caster instanceof Player player ? player : null;

        while (placedThisTick < maxPerTick && this.nextIndex < this.currentFloorPattern.size()) {
            BlockPos target = this.currentFloorPattern.get(this.nextIndex);
            this.nextIndex++;

            if (!serverLevel.isLoaded(target)) {
                continue;
            }

            BlockState current = serverLevel.getBlockState(target);
            if (!shouldReplaceBlock(serverLevel, target, current, this.waterPower)) {
                continue;
            }

            BlockState waterState = Blocks.WATER.defaultBlockState();
            if (!BlockProtectionUtil.canBlockBePlaced(serverLevel, target, waterState, claimActor)) {
                continue;
            }

            serverLevel.setBlock(target, waterState, 3);
            WaterConvergenceParticleHelper.spawnSplashParticle(serverLevel, target);

            if (serverLevel.random.nextFloat() < 0.03f) {
                double x = target.getX() + 0.5;
                double y = target.getY() + 0.5;
                double z = target.getZ() + 0.5;

                int dx = target.getX() - this.centerPos.getX();
                int dz = target.getZ() - this.centerPos.getZ();
                double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                int radius = getRadius();
                double distanceRatio = Math.min(1.0, horizontalDist / radius);
                float pitch = 1.5f - (float) (distanceRatio);

                serverLevel.playSound(null, x, y, z, ModSounds.SPLASH_FAST.get(), SoundSource.BLOCKS, 1.0f, pitch);
            }

            consumeManaForBlock();
            placedThisTick++;
        }
    }

    private void ensureInitialized() {
        if (this.centerPos != null) {
            return;
        }
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        this.centerPos = getSphereCenterBlockPos();
        this.nextIndex = 0;
        this.currentY = Integer.MIN_VALUE;
        this.ticksOnCurrentY = 0;
    }

    private void computeCurrentFloor(ServerLevel serverLevel) {
        int radius = getRadius();
        this.currentFloorPattern = WaterConvergencePattern.floodFillFloor(serverLevel, this.centerPos, this.currentY,
                radius);
        this.nextIndex = 0;
    }

    private void advanceToNextYLevel(ServerLevel serverLevel) {
        this.currentY++;
        computeCurrentFloor(serverLevel);
    }

    protected boolean shouldReplaceBlock(ServerLevel level, BlockPos pos, BlockState current, float waterPower) {
        return current.isAir();
    }

    protected int getMaxPlacementsPerTick(float waterPower) {
        float toPlace = PLACEMENT_MULTIPLIER * (1.0f + waterPower / 2.0f);
        return (int) toPlace;
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

    private void consumeManaForBlock() {
        if (this.casterUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity caster = serverLevel.getEntity(this.casterUuid) instanceof LivingEntity living ? living : null;
        boolean isCreative = caster instanceof Player player && player.getAbilities().instabuild;

        if (this.consumedMana < FREE_MANA_AMOUNT) {
            double remainingFree = FREE_MANA_AMOUNT - this.consumedMana;
            if (MANA_COST_PER_BLOCK <= remainingFree) {
                this.consumedMana += MANA_COST_PER_BLOCK;
                return;
            } else {
                double freeAmount = remainingFree;
                this.consumedMana += freeAmount;
                double remainingCost = MANA_COST_PER_BLOCK - freeAmount;

                if (caster instanceof Player) {
                    IManaCap manaCap = CapabilityRegistry.getMana(caster);
                    if (manaCap != null && manaCap.getCurrentMana() >= remainingCost) {
                        manaCap.removeMana(remainingCost);
                        this.consumedMana += remainingCost;
                    } else if (!isCreative) {
                        this.discard();
                    } else {
                        this.consumedMana += remainingCost;
                    }
                }
                return;
            }
        }

        if (caster instanceof Player) {
            IManaCap manaCap = CapabilityRegistry.getMana(caster);
            if (manaCap != null && manaCap.getCurrentMana() >= MANA_COST_PER_BLOCK) {
                manaCap.removeMana(MANA_COST_PER_BLOCK);
                this.consumedMana += MANA_COST_PER_BLOCK;
            } else if (!isCreative) {
                this.discard();
            } else {
                this.consumedMana += MANA_COST_PER_BLOCK;
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
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
        if (compound.contains("current_y")) {
            this.currentY = compound.getInt("current_y");
        }
        if (compound.contains("ticks_on_current_y")) {
            this.ticksOnCurrentY = compound.getInt("ticks_on_current_y");
        }
        if (compound.contains("center_x") && compound.contains("center_y") && compound.contains("center_z")) {
            this.centerPos = new BlockPos(compound.getInt("center_x"), compound.getInt("center_y"),
                    compound.getInt("center_z"));
        }
        if (compound.contains("consumed_mana")) {
            this.consumedMana = compound.getDouble("consumed_mana");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("next_index", this.nextIndex);
        compound.putFloat("water_power", this.waterPower);
        if (this.casterUuid != null) {
            compound.putUUID("caster_uuid", this.casterUuid);
        }
        compound.putInt("radius", getRadius());
        compound.putInt("current_y", this.currentY);
        compound.putInt("ticks_on_current_y", this.ticksOnCurrentY);
        if (this.centerPos != null) {
            compound.putInt("center_x", this.centerPos.getX());
            compound.putInt("center_y", this.centerPos.getY());
            compound.putInt("center_z", this.centerPos.getZ());
        }
        compound.putDouble("consumed_mana", this.consumedMana);
    }
}
