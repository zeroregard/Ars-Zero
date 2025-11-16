package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class WindVoxelEntity extends BaseVoxelEntity {
    
    public WindVoxelEntity(EntityType<? extends WindVoxelEntity> entityType, Level level) {
        super(entityType, level);
    }
    
    public WindVoxelEntity(Level level, double x, double y, double z, int lifetime) {
        this(ModEntities.WIND_VOXEL_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.setLifetime(lifetime);
    }
    
    @Override
    public int getColor() {
        return 0xA7E6FF;
    }
    
    @Override
    public boolean isEmissive() {
        return false;
    }
    
    @Override
    protected net.minecraft.sounds.SoundEvent getSpawnSound() {
        return null;
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof BaseVoxelEntity) {
            super.onHitEntity(result);
            return;
        }
        
        Vec3 push = this.getDeltaMovement();
        if (!push.equals(Vec3.ZERO)) {
            if (result.getEntity() instanceof LivingEntity living) {
                Vec3 current = living.getDeltaMovement();
                living.setDeltaMovement(current.add(push));
                living.hurtMarked = true;
            } else {
                Vec3 current = result.getEntity().getDeltaMovement();
                result.getEntity().setDeltaMovement(current.add(push));
                result.getEntity().hasImpulse = true;
            }
        }
        
        spawnHitParticles(result.getLocation());
        this.discard();
    }
    
    @Override
    protected void onBlockCollision(BlockHitResult blockHit) {
        BlockPos hitPos = blockHit.getBlockPos();
        BlockState state = this.level().getBlockState(hitPos);
        
        if (!this.level().isClientSide) {
            if (state.is(Blocks.FIRE)) {
                spawnFireAndWindParticles(Vec3.atCenterOf(hitPos));
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.5f, Level.ExplosionInteraction.TNT);
                ((ServerLevel)this.level()).playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.8f, 1.2f);
            } else if (state.getFluidState().isSourceOfType(net.minecraft.world.level.material.Fluids.WATER) || state.is(Blocks.WATER)) {
                spawnWaterAndWindParticles(Vec3.atCenterOf(hitPos));
            } else {
                spawnWindParticles(Vec3.atCenterOf(hitPos), 12);
            }
        }
    }
    
    @Override
    protected void spawnHitParticles(Vec3 location) {
        if (!this.level().isClientSide) {
            spawnWindParticles(location, 20);
        }
    }
    
    @Override
    protected ParticleOptions getAmbientParticle() {
        return ParticleTypes.CLOUD;
    }
    
    @Override
    public void tick() {
        this.setNoGravityCustom(true);
        super.tick();
        if (!this.level().isClientSide) {
            Vec3 motion = this.getDeltaMovement();
            double speed = motion.length();
            if (speed > 0.05) {
                int count = Math.min(6, (int)Math.ceil(speed * 10));
                for (int i = 0; i < count; i++) {
                    double ox = (this.random.nextDouble() - 0.5) * 0.2;
                    double oy = (this.random.nextDouble() - 0.5) * 0.2;
                    double oz = (this.random.nextDouble() - 0.5) * 0.2;
                    ((ServerLevel)this.level()).sendParticles(ParticleTypes.CLOUD, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 1, 0.0, 0.0, 0.0, 0.01);
                }
            }
        }
    }
    
    private void spawnWindParticles(Vec3 location, int count) {
        for (int i = 0; i < count; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 0.4;
            double oy = (this.random.nextDouble() - 0.5) * 0.4;
            double oz = (this.random.nextDouble() - 0.5) * 0.4;
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.CLOUD, location.x + ox, location.y + oy, location.z + oz, 1, 0.0, 0.0, 0.0, 0.01);
        }
    }
    
    private void spawnFireAndWindParticles(Vec3 location) {
        spawnWindParticles(location, 16);
        for (int i = 0; i < 10; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 0.3;
            double oy = (this.random.nextDouble() - 0.5) * 0.3;
            double oz = (this.random.nextDouble() - 0.5) * 0.3;
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.FLAME, location.x + ox, location.y + oy, location.z + oz, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
    
    private void spawnWaterAndWindParticles(Vec3 location) {
        spawnWindParticles(location, 16);
        for (int i = 0; i < 10; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 0.3;
            double oy = (this.random.nextDouble() - 0.5) * 0.3;
            double oz = (this.random.nextDouble() - 0.5) * 0.3;
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.SPLASH, location.x + ox, location.y + oy, location.z + oz, 1, 0.0, 0.0, 0.0, 0.1);
        }
    }
}


