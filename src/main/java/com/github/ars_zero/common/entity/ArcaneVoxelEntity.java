package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.setup.registry.SoundRegistry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class ArcaneVoxelEntity extends BaseVoxelEntity implements CompressibleEntity {
    
    private static final int COLOR = 0x8A2BE2;
    
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
        return COLOR;
    }
    
    @Override
    public boolean isEmissive() {
        return true;
    }
    
    protected SoundEvent getSpawnSound() {
        return SoundRegistry.GAIA_FAMILY.get();
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
        this.compressionLevel = compressionLevel;
    }
    
    @Override
    public float getCompressionLevel() {
        return this.compressionLevel;
    }
    
    @Override
    public void setEmissiveIntensity(float intensity) {
        this.emissiveIntensity = intensity;
    }
    
    @Override
    public float getEmissiveIntensity() {
        return this.emissiveIntensity;
    }
    
    @Override
    public void setDamageEnabled(boolean enabled) {
        this.damageEnabled = enabled;
    }
    
    @Override
    public boolean isDamageEnabled() {
        return this.damageEnabled;
    }
    
    @Override
    public int getCompressedColor() {
        return 0x87CEEB;
    }
}

