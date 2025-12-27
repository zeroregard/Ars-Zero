package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
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
    
    @Override
    protected net.minecraft.core.particles.ParticleOptions getAmbientParticle() {
        Vector3f color = new Vector3f(0.54f, 0.17f, 0.89f);
        return new DustParticleOptions(color, 0.8f);
    }
    
    @Override
    protected void onBlockCollision(BlockHitResult blockHit) {
        if (handlePhysicalCollision(blockHit)) {
            return;
        }
        if (!this.level().isClientSide) {
            Vec3 location = blockHit.getLocation();
            this.level().playSound(null, location.x, location.y, location.z, 
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.5f, 1.2f + this.random.nextFloat() * 0.3f);
        }
        spawnHitParticles(blockHit.getLocation());
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hit = result.getEntity();
        if (hit instanceof BaseVoxelEntity) {
            super.onHitEntity(result);
            return;
        }
        if (!this.level().isClientSide) {
            Vec3 location = result.getLocation();
            this.level().playSound(null, location.x, location.y, location.z, 
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.5f, 1.2f + this.random.nextFloat() * 0.3f);
        }
        spawnHitParticles(result.getLocation());
        resolveAndDiscard(result);
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

