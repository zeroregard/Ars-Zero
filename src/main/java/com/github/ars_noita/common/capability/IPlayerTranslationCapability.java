package com.github.ars_noita.common.capability;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Capability interface for storing player translation data.
 * This replaces the static HashMap approach with a proper Minecraft capability.
 */
public interface IPlayerTranslationCapability {
    
    /**
     * Check if the player has an active translation
     */
    boolean hasActiveTranslation();
    
    /**
     * Get the target entity being translated
     */
    Entity getTargetEntity();
    
    /**
     * Set the target entity for translation
     */
    void setTargetEntity(Entity entity);
    
    /**
     * Get the initial relative position
     */
    Vec3 getInitialRelativePos();
    
    /**
     * Set the initial relative position
     */
    void setInitialRelativePos(Vec3 pos);
    
    /**
     * Get the initial yaw
     */
    float getInitialYaw();
    
    /**
     * Set the initial yaw
     */
    void setInitialYaw(float yaw);
    
    /**
     * Get the initial pitch
     */
    float getInitialPitch();
    
    /**
     * Set the initial pitch
     */
    void setInitialPitch(float pitch);
    
    /**
     * Get the duration in ticks
     */
    int getDuration();
    
    /**
     * Set the duration in ticks
     */
    void setDuration(int duration);
    
    /**
     * Get remaining ticks
     */
    int getRemainingTicks();
    
    /**
     * Set remaining ticks
     */
    void setRemainingTicks(int ticks);
    
    /**
     * Decrement remaining ticks by 1
     */
    void decrementTicks();
    
    /**
     * Clear all translation data
     */
    void clearTranslation();
}
