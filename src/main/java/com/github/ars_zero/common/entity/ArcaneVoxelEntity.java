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

public class ArcaneVoxelEntity extends BaseVoxelEntity {
    
    private static final int COLOR = 0x8A2BE2;
    
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
}

