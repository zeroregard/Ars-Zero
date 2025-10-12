package com.github.ars_zero.common.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Implementation of IPlayerTranslationCapability.
 * Stores translation data as NBT for persistence.
 */
public class PlayerTranslationCapability implements IPlayerTranslationCapability {
    
    private static final String TAG_TARGET_ENTITY_ID = "target_entity_id";
    private static final String TAG_INITIAL_RELATIVE_X = "initial_relative_x";
    private static final String TAG_INITIAL_RELATIVE_Y = "initial_relative_y";
    private static final String TAG_INITIAL_RELATIVE_Z = "initial_relative_z";
    private static final String TAG_INITIAL_YAW = "initial_yaw";
    private static final String TAG_INITIAL_PITCH = "initial_pitch";
    private static final String TAG_DURATION = "duration";
    private static final String TAG_REMAINING_TICKS = "remaining_ticks";
    
    private Entity targetEntity;
    private Vec3 initialRelativePos = Vec3.ZERO;
    private float initialYaw = 0.0f;
    private float initialPitch = 0.0f;
    private int duration = 0;
    private int remainingTicks = 0;
    
    @Override
    public boolean hasActiveTranslation() {
        return targetEntity != null && remainingTicks > 0;
    }
    
    @Override
    public Entity getTargetEntity() {
        return targetEntity;
    }
    
    @Override
    public void setTargetEntity(Entity entity) {
        this.targetEntity = entity;
    }
    
    @Override
    public Vec3 getInitialRelativePos() {
        return initialRelativePos;
    }
    
    @Override
    public void setInitialRelativePos(Vec3 pos) {
        this.initialRelativePos = pos;
    }
    
    @Override
    public float getInitialYaw() {
        return initialYaw;
    }
    
    @Override
    public void setInitialYaw(float yaw) {
        this.initialYaw = yaw;
    }
    
    @Override
    public float getInitialPitch() {
        return initialPitch;
    }
    
    @Override
    public void setInitialPitch(float pitch) {
        this.initialPitch = pitch;
    }
    
    @Override
    public int getDuration() {
        return duration;
    }
    
    @Override
    public void setDuration(int duration) {
        this.duration = duration;
        this.remainingTicks = duration;
    }
    
    @Override
    public int getRemainingTicks() {
        return remainingTicks;
    }
    
    @Override
    public void setRemainingTicks(int ticks) {
        this.remainingTicks = ticks;
    }
    
    @Override
    public void decrementTicks() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }
    
    @Override
    public void clearTranslation() {
        this.targetEntity = null;
        this.initialRelativePos = Vec3.ZERO;
        this.initialYaw = 0.0f;
        this.initialPitch = 0.0f;
        this.duration = 0;
        this.remainingTicks = 0;
    }
    
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        
        if (targetEntity != null) {
            tag.putInt(TAG_TARGET_ENTITY_ID, targetEntity.getId());
        }
        
        tag.putDouble(TAG_INITIAL_RELATIVE_X, initialRelativePos.x);
        tag.putDouble(TAG_INITIAL_RELATIVE_Y, initialRelativePos.y);
        tag.putDouble(TAG_INITIAL_RELATIVE_Z, initialRelativePos.z);
        tag.putFloat(TAG_INITIAL_YAW, initialYaw);
        tag.putFloat(TAG_INITIAL_PITCH, initialPitch);
        tag.putInt(TAG_DURATION, duration);
        tag.putInt(TAG_REMAINING_TICKS, remainingTicks);
        
        return tag;
    }
    
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains(TAG_TARGET_ENTITY_ID)) {
            // Note: We can't restore the entity reference here as we don't have access to the world
            // The entity will need to be looked up by ID when needed
            // For now, we'll leave targetEntity as null and handle it in the capability provider
        }
        
        double x = tag.getDouble(TAG_INITIAL_RELATIVE_X);
        double y = tag.getDouble(TAG_INITIAL_RELATIVE_Y);
        double z = tag.getDouble(TAG_INITIAL_RELATIVE_Z);
        this.initialRelativePos = new Vec3(x, y, z);
        
        this.initialYaw = tag.getFloat(TAG_INITIAL_YAW);
        this.initialPitch = tag.getFloat(TAG_INITIAL_PITCH);
        this.duration = tag.getInt(TAG_DURATION);
        this.remainingTicks = tag.getInt(TAG_REMAINING_TICKS);
    }
}
