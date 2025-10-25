package com.github.ars_zero.common.entity.interaction;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class VoxelInteractionResult {
    
    public enum ActionType {
        DISCARD,
        RESIZE,
        CONTINUE,
        REPEL,
        RESOLVE
    }
    
    private final ActionType primaryAction;
    private final ActionType secondaryAction;
    private final float primaryNewSize;
    private final float secondaryNewSize;
    private final ParticleOptions particleType;
    private final int particleCount;
    private final SoundEvent soundEvent;
    private final Vec3 interactionLocation;
    private final Vec3 primaryRepelDirection;
    private final Vec3 secondaryRepelDirection;
    private final float repelForce;
    
    private VoxelInteractionResult(Builder builder) {
        this.primaryAction = builder.primaryAction;
        this.secondaryAction = builder.secondaryAction;
        this.primaryNewSize = builder.primaryNewSize;
        this.secondaryNewSize = builder.secondaryNewSize;
        this.particleType = builder.particleType;
        this.particleCount = builder.particleCount;
        this.soundEvent = builder.soundEvent;
        this.interactionLocation = builder.interactionLocation;
        this.primaryRepelDirection = builder.primaryRepelDirection;
        this.secondaryRepelDirection = builder.secondaryRepelDirection;
        this.repelForce = builder.repelForce;
    }
    
    public ActionType getPrimaryAction() {
        return primaryAction;
    }
    
    public ActionType getSecondaryAction() {
        return secondaryAction;
    }
    
    public float getPrimaryNewSize() {
        return primaryNewSize;
    }
    
    public float getSecondaryNewSize() {
        return secondaryNewSize;
    }
    
    public Optional<ParticleOptions> getParticleType() {
        return Optional.ofNullable(particleType);
    }
    
    public int getParticleCount() {
        return particleCount;
    }
    
    public Optional<SoundEvent> getSoundEvent() {
        return Optional.ofNullable(soundEvent);
    }
    
    public Vec3 getInteractionLocation() {
        return interactionLocation;
    }
    
    public Vec3 getPrimaryRepelDirection() {
        return primaryRepelDirection;
    }
    
    public Vec3 getSecondaryRepelDirection() {
        return secondaryRepelDirection;
    }
    
    public float getRepelForce() {
        return repelForce;
    }
    
    public static Builder builder(Vec3 location) {
        return new Builder(location);
    }
    
    public static class Builder {
        private ActionType primaryAction = ActionType.DISCARD;
        private ActionType secondaryAction = ActionType.DISCARD;
        private float primaryNewSize;
        private float secondaryNewSize;
        private ParticleOptions particleType;
        private int particleCount = 20;
        private SoundEvent soundEvent;
        private final Vec3 interactionLocation;
        private Vec3 primaryRepelDirection;
        private Vec3 secondaryRepelDirection;
        private float repelForce = 0.5f;
        
        private Builder(Vec3 location) {
            this.interactionLocation = location;
        }
        
        public Builder primaryAction(ActionType action) {
            this.primaryAction = action;
            return this;
        }
        
        public Builder secondaryAction(ActionType action) {
            this.secondaryAction = action;
            return this;
        }
        
        public Builder primaryNewSize(float size) {
            this.primaryNewSize = size;
            return this;
        }
        
        public Builder secondaryNewSize(float size) {
            this.secondaryNewSize = size;
            return this;
        }
        
        public Builder particles(ParticleOptions particle, int count) {
            this.particleType = particle;
            this.particleCount = count;
            return this;
        }
        
        public Builder sound(SoundEvent sound) {
            this.soundEvent = sound;
            return this;
        }
        
        public Builder repelDirections(Vec3 primaryDir, Vec3 secondaryDir, float force) {
            this.primaryRepelDirection = primaryDir;
            this.secondaryRepelDirection = secondaryDir;
            this.repelForce = force;
            return this;
        }
        
        public VoxelInteractionResult build() {
            return new VoxelInteractionResult(this);
        }
    }
}


