package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ArcaneVoxelEntity extends BaseVoxelEntity implements CompressibleVoxelEntity {
    
    private static final int COLOR = 0x8A2BE2;
    private static final int COMPRESSED_COLOR = 0x87CEEB;
    
    private float compressionLevel = 0.0f;
    private float emissiveIntensity = 0.0f;
    private boolean damageEnabled = false;
    
    public ArcaneVoxelEntity(EntityType<? extends ArcaneVoxelEntity> entityType, Level level) {
        super(entityType, level);
    }
    
    public ArcaneVoxelEntity(Level level, double x, double y, double z, int lifetime) {
        this(ModEntities.ARCANE_VOXEL_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.setLifetime(lifetime);
    }
    
    @Override
    public int getColor() {
        if (compressionLevel > 0.5f) {
            return getCompressedColor();
        }
        return COLOR;
    }
    
    @Override
    public int getCompressedColor() {
        float t = Math.min(compressionLevel * 2.0f, 1.0f);
        int r = (int) (0x8A + (0x87 - 0x8A) * t);
        int g = (int) (0x2B + (0xCE - 0x2B) * t);
        int b = (int) (0xE2 + (0xEB - 0xE2) * t);
        return (r << 16) | (g << 8) | b;
    }
    
    @Override
    public boolean isEmissive() {
        return true;
    }
    
    @Override
    protected net.minecraft.core.particles.ParticleOptions getAmbientParticle() {
        Vector3f color = new Vector3f(0.54f, 0.17f, 0.89f);
        return new DustParticleOptions(color, 0.8f);
    }
    
    @Override
    protected void spawnHitParticles(Vec3 location) {
        if (!this.level().isClientSide) {
            net.minecraft.core.particles.ParticleOptions particleOptions = getAmbientParticle();
            
            for (int i = 0; i < 12; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.4;
                double offsetY = (this.random.nextDouble() - 0.5) * 0.4;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.4;
                ((ServerLevel) this.level()).sendParticles(
                    particleOptions,
                    location.x + offsetX,
                    location.y + offsetY,
                    location.z + offsetZ,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
                );
            }
        }
    }
    
    @Override
    public void setCompressionLevel(float compressionLevel) {
        this.compressionLevel = Math.max(0.0f, Math.min(1.0f, compressionLevel));
    }
    
    @Override
    public float getCompressionLevel() {
        return compressionLevel;
    }
    
    @Override
    public void setEmissiveIntensity(float intensity) {
        this.emissiveIntensity = Math.max(0.0f, intensity);
    }
    
    @Override
    public float getEmissiveIntensity() {
        return emissiveIntensity;
    }
    
    @Override
    public void setDamageEnabled(boolean enabled) {
        this.damageEnabled = enabled;
    }
    
    @Override
    public boolean isDamageEnabled() {
        return damageEnabled;
    }
}

