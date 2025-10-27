package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.setup.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class FireVoxelEntity extends BaseVoxelEntity {
    
    private static final int COLOR = 0xFF6A00;
    
    public FireVoxelEntity(EntityType<? extends FireVoxelEntity> entityType, Level level) {
        super(entityType, level);
    }
    
    public FireVoxelEntity(Level level, double x, double y, double z, int lifetime) {
        this(ModEntities.FIRE_VOXEL_ENTITY.get(), level);
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
    protected void onBlockCollision(BlockHitResult blockHit) {
        BlockPos hitPos = blockHit.getBlockPos();
        BlockState hitState = this.level().getBlockState(hitPos);
        
        if (hitState.getBlock() == Blocks.WATER) {
            if (canEvaporateWater(hitState)) {
                this.level().setBlock(hitPos, Blocks.AIR.defaultBlockState(), 3);
                spawnEvaporationParticles(Vec3.atCenterOf(hitPos));
            }
            return;
        }
        
        BlockPos placePos = hitPos.relative(blockHit.getDirection());
        BlockState placeState = this.level().getBlockState(placePos);
        
        if (placeState.getBlock() == Blocks.WATER) {
            if (canEvaporateWater(placeState)) {
                this.level().setBlock(placePos, Blocks.AIR.defaultBlockState(), 3);
                spawnEvaporationParticles(Vec3.atCenterOf(placePos));
            }
        } else if (placeState.isAir()) {
            this.level().setBlock(placePos, Blocks.FIRE.defaultBlockState(), 3);
        }
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    protected void onHitEntity(net.minecraft.world.phys.EntityHitResult result) {
        if (result.getEntity() instanceof BaseVoxelEntity) {
            super.onHitEntity(result);
            return;
        }
        
        if (!this.level().isClientSide) {
            Vec3 hitLocation = result.getLocation();
            
            BlockPos centerPos = new BlockPos(
                (int) Math.round(hitLocation.x),
                (int) Math.round(hitLocation.y),
                (int) Math.round(hitLocation.z)
            );
            
            for (BlockPos pos : BlockPos.betweenClosed(centerPos.offset(-1, -1, -1), centerPos.offset(1, 1, 1))) {
                BlockState state = this.level().getBlockState(pos);
                if (state.getBlock() == Blocks.WATER && canEvaporateWater(state)) {
                    this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    spawnEvaporationParticles(Vec3.atCenterOf(pos));
                    break;
                } else if (state.isAir()) {
                    this.level().setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                    break;
                }
            }
        }
        this.discard();
    }
    
    private boolean canEvaporateWater(BlockState waterState) {
        if (waterState.getBlock() != Blocks.WATER) {
            return false;
        }
        
        int waterLevel = waterState.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL);
        
        if (waterLevel == 0) {
            return this.getSize() >= 1.0f;
        }
        
        return true;
    }
    
    private void spawnEvaporationParticles(Vec3 location) {
        if (!this.level().isClientSide) {
            for (int i = 0; i < 20; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
                double offsetY = this.random.nextDouble() * 0.5;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;
                ((ServerLevel) this.level()).sendParticles(
                    ParticleTypes.CLOUD,
                    location.x + offsetX,
                    location.y + offsetY,
                    location.z + offsetZ,
                    1,
                    0.0, 0.1, 0.0,
                    0.02
                );
            }
            for (int i = 0; i < 10; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.3;
                double offsetY = (this.random.nextDouble() - 0.5) * 0.3;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.3;
                ((ServerLevel) this.level()).sendParticles(
                    ParticleTypes.SMOKE,
                    location.x + offsetX,
                    location.y + offsetY,
                    location.z + offsetZ,
                    1,
                    0.0, 0.05, 0.0,
                    0.01
                );
            }
        }
    }
    
    private int calculateParticleCount() {
        float size = this.getSize();
        float ratio = size / 1.0f;
        
        int baseCount = (int) (ratio * 32);
        return Math.min(baseCount, 32);
    }
    
    @Override
    protected net.minecraft.core.particles.ParticleOptions getAmbientParticle() {
        return this.random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SMOKE;
    }
    
    protected SoundEvent getSpawnSound() {
        return SoundRegistry.FIRE_FAMILY.get();
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide) {
            Vec3 motion = this.getDeltaMovement();
            double speed = motion.length();
            
            if (speed > 0.05) {
                int trailCount = (int) (speed * 10);
                for (int i = 0; i < Math.min(trailCount, 5); i++) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 0.2;
                    double offsetY = (this.random.nextDouble() - 0.5) * 0.2;
                    double offsetZ = (this.random.nextDouble() - 0.5) * 0.2;
                    
                    ((ServerLevel) this.level()).sendParticles(
                        ParticleTypes.FLAME,
                        this.getX() + offsetX,
                        this.getY() + offsetY,
                        this.getZ() + offsetZ,
                        1,
                        0.0, 0.0, 0.0,
                        0.01
                    );
                    
                    if (this.random.nextFloat() < 0.3f) {
                        ((ServerLevel) this.level()).sendParticles(
                            ParticleTypes.SMOKE,
                            this.getX() + offsetX,
                            this.getY() + offsetY,
                            this.getZ() + offsetZ,
                            1,
                            0.0, 0.0, 0.0,
                            0.005
                        );
                    }
                }
            }
            
            if (this.age % 5 == 0) {
                for (int i = 0; i < 2; i++) {
                    double offsetX = (this.random.nextDouble() - 0.5) * this.getSize() * 0.5;
                    double offsetY = (this.random.nextDouble() - 0.5) * this.getSize() * 0.5;
                    double offsetZ = (this.random.nextDouble() - 0.5) * this.getSize() * 0.5;
                    
                    ((ServerLevel) this.level()).sendParticles(
                        ParticleTypes.SMOKE,
                        this.getX() + offsetX,
                        this.getY() + offsetY,
                        this.getZ() + offsetZ,
                        1,
                        0.0, 0.02, 0.0,
                        0.01
                    );
                }
            }
        }
    }
    
    @Override
    protected void spawnHitParticles(Vec3 location) {
        if (!this.level().isClientSide) {
            int particleCount = calculateParticleCount();
            
            for (int i = 0; i < particleCount; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.3;
                double offsetY = (this.random.nextDouble() - 0.5) * 0.3;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.3;
                ((ServerLevel) this.level()).sendParticles(
                    ParticleTypes.FLAME,
                    location.x + offsetX,
                    location.y + offsetY,
                    location.z + offsetZ,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
                );
            }
            for (int i = 0; i < particleCount / 2; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.2;
                double offsetY = (this.random.nextDouble() - 0.5) * 0.2;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.2;
                ((ServerLevel) this.level()).sendParticles(
                    ParticleTypes.SMOKE,
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

