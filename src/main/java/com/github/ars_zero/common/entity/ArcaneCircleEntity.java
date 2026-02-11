package com.github.ars_zero.common.entity;

import com.github.ars_zero.common.casting.CastingStyle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.UUID;

public class ArcaneCircleEntity extends AbstractConvergenceEntity {
    public static final int FADE_TICKS = 5;

    private static final EntityDataAccessor<Integer> DATA_SPAWN_TICK = SynchedEntityData.defineId(ArcaneCircleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(ArcaneCircleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_Y_ROT = SynchedEntityData.defineId(ArcaneCircleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_X_ROT = SynchedEntityData.defineId(ArcaneCircleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<CompoundTag> DATA_STYLE = SynchedEntityData.defineId(ArcaneCircleEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<String> DATA_SCHOOL_ID = SynchedEntityData.defineId(ArcaneCircleEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_PENDING_DISCARD = SynchedEntityData.defineId(ArcaneCircleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_FADE_OUT_LIFESPAN = SynchedEntityData.defineId(ArcaneCircleEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID casterUUID;
    private int spawnTick = 0;
    private CastingStyle style;

    public ArcaneCircleEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noCulling = true;
    }

    public void initialize(@Nullable LivingEntity caster, CastingStyle style) {
        this.casterUUID = caster != null ? caster.getUUID() : null;
        this.style = style;
        this.spawnTick = this.tickCount;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_SPAWN_TICK, this.spawnTick);
            this.entityData.set(DATA_COLOR, style.getColor());
            this.entityData.set(DATA_STYLE, style.save());
        }
    }

    @Nullable
    public UUID getCasterUUID() {
        if (this.level().isClientSide) {
            return this.casterUUID;
        }
        return this.casterUUID;
    }

    public int getSpawnTick() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_SPAWN_TICK);
        }
        return this.spawnTick;
    }

    public int getColor() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_COLOR);
        }
        return style != null ? style.getColor() : 0xFFFFFF;
    }

    public CastingStyle getStyle() {
        if (this.level().isClientSide) {
            CompoundTag styleTag = this.entityData.get(DATA_STYLE);
            return CastingStyle.load(styleTag);
        }
        return style;
    }

    public float getSyncedYRot() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_Y_ROT);
        }
        return this.getYRot();
    }

    public float getSyncedXRot() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_X_ROT);
        }
        return this.getXRot();
    }

    public void setSyncedRotation(float yRot, float xRot) {
        this.setYRot(yRot);
        this.setXRot(xRot);
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_Y_ROT, yRot);
            this.entityData.set(DATA_X_ROT, xRot);
        }
    }

    @Nullable
    public String getCurrentSchoolId() {
        String id = this.entityData.get(DATA_SCHOOL_ID);
        return id == null || id.isEmpty() ? null : id;
    }

    public void setCurrentSchoolId(@Nullable String schoolId) {
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_SCHOOL_ID, schoolId != null ? schoolId : "");
        }
    }

    public boolean isPendingDiscard() {
        return this.entityData.get(DATA_PENDING_DISCARD);
    }

    public int getFadeOutLifespan() {
        return this.entityData.get(DATA_FADE_OUT_LIFESPAN);
    }

    public void scheduleDiscard() {
        if (!this.level().isClientSide && !this.entityData.get(DATA_PENDING_DISCARD)) {
            this.entityData.set(DATA_PENDING_DISCARD, true);
            this.entityData.set(DATA_FADE_OUT_LIFESPAN, FADE_TICKS);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.entityData.get(DATA_PENDING_DISCARD)) {
                int lifespan = this.entityData.get(DATA_FADE_OUT_LIFESPAN) - 1;
                this.entityData.set(DATA_FADE_OUT_LIFESPAN, lifespan);
                if (lifespan <= 0) {
                    this.discard();
                    return;
                }
                return;
            }
        }

        if (this.level().isClientSide) {
            if (this.style != null && this.style.getPlacement() == CastingStyle.Placement.NEAR) {
                this.setYRot(this.getSyncedYRot());
                this.setXRot(this.getSyncedXRot());
            }
        } else if (this.casterUUID != null && this.style != null) {
            updatePositionAndRotation();
        }
    }

    private void updatePositionAndRotation() {
        if (this.level() instanceof ServerLevel serverLevel && this.casterUUID != null) {
            Player caster = serverLevel.getServer().getPlayerList().getPlayer(this.casterUUID);
            if (caster == null || !caster.isAlive()) {
                this.scheduleDiscard();
                return;
            }

            Vec3 targetPos;
            if (style.getPlacement() == CastingStyle.Placement.FEET) {
                double y = caster.blockPosition().below().getY() + 1;
                targetPos = new Vec3(caster.position().x, y, caster.position().z);
            } else {
                Vec3 eyePos = caster.getEyePosition(1.0f);
                Vec3 lookVec = caster.getLookAngle();
                targetPos = eyePos.add(lookVec.scale(1.0));
                
                double lookX = lookVec.x;
                double lookY = lookVec.y;
                double lookZ = lookVec.z;
                
                double horizontalLength = Math.sqrt(lookX * lookX + lookZ * lookZ);
                float yaw = (float) (Mth.atan2(-lookX, lookZ) * 180.0 / Math.PI);
                float pitch = (float) (Mth.atan2(-lookY, horizontalLength) * 180.0 / Math.PI);
                
                this.setSyncedRotation(yaw, pitch);
            }

            this.setPos(targetPos.x, targetPos.y, targetPos.z);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public float getPickRadius() {
        return 0.0f;
    }

    @Override
    public boolean canCollideWith(net.minecraft.world.entity.Entity entity) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isInvisible() {
        return true;
    }

    @Override
    public boolean isInvisibleTo(Player player) {
        return true;
    }

    @Override
    protected void onLifespanReached() {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, this::idlePredicate));
    }

    private PlayState idlePredicate(AnimationState<ArcaneCircleEntity> state) {
        CastingStyle s = getStyle();
        float speed = (s != null) ? s.getSpeed() : 1.0f;
        state.getController().setAnimationSpeed(speed);
        return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPAWN_TICK, 0);
        builder.define(DATA_COLOR, 0xFFFFFF);
        builder.define(DATA_Y_ROT, 0.0f);
        builder.define(DATA_X_ROT, 0.0f);
        builder.define(DATA_STYLE, new CompoundTag());
        builder.define(DATA_SCHOOL_ID, "");
        builder.define(DATA_PENDING_DISCARD, false);
        builder.define(DATA_FADE_OUT_LIFESPAN, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("casterUUID")) {
            this.casterUUID = compound.getUUID("casterUUID");
        }
        this.spawnTick = compound.getInt("spawnTick");
        if (compound.contains("style")) {
            this.style = CastingStyle.load(compound.getCompound("style"));
        }
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_PENDING_DISCARD, compound.getBoolean("pendingDiscard"));
            this.entityData.set(DATA_FADE_OUT_LIFESPAN, compound.getInt("fadeOutLifespan"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.casterUUID != null) {
            compound.putUUID("casterUUID", this.casterUUID);
        }
        compound.putInt("spawnTick", this.spawnTick);
        if (this.style != null) {
            compound.put("style", this.style.save());
        }
        compound.putBoolean("pendingDiscard", this.entityData.get(DATA_PENDING_DISCARD));
        compound.putInt("fadeOutLifespan", this.entityData.get(DATA_FADE_OUT_LIFESPAN));
    }
}
