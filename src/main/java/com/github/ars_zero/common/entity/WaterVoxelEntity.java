package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class WaterVoxelEntity extends BaseVoxelEntity {
    
    private static final int COLOR = 0x3F76E4;
    private boolean hasPlayedSpawnSound = false;
    
    public WaterVoxelEntity(EntityType<? extends WaterVoxelEntity> entityType, Level level) {
        super(entityType, level);
    }
    
    public WaterVoxelEntity(Level level, double x, double y, double z, int lifetime) {
        this(ModEntities.WATER_VOXEL_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.setLifetime(lifetime);
    }
    
    @Override
    public int getColor() {
        return COLOR;
    }
    
    @Override
    public boolean isEmissive() {
        return false;
    }
    
    @Override
    protected void onBlockCollision(BlockHitResult blockHit) {
        BlockPos pos = blockHit.getBlockPos().relative(blockHit.getDirection());
        if (this.level().getBlockState(pos).isAir()) {
            int waterLevel = calculateWaterLevel();
            this.level().setBlock(pos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, waterLevel), 3);
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
            
            int waterLevel = calculateWaterLevel();
            for (BlockPos pos : BlockPos.betweenClosed(centerPos.offset(-1, -1, -1), centerPos.offset(1, 1, 1))) {
                if (this.level().getBlockState(pos).isAir()) {
                    this.level().setBlock(pos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, waterLevel), 3);
                    break;
                }
            }
        }
        this.discard();
    }
    
    private int calculateWaterLevel() {
        float size = this.getSize();
        float ratio = size / 1.0f;
        
        if (ratio >= 1.0f) {
            return 0;
        } else if (ratio >= 0.875f) {
            return 1;
        } else if (ratio >= 0.75f) {
            return 2;
        } else if (ratio >= 0.625f) {
            return 3;
        } else if (ratio >= 0.5f) {
            return 4;
        } else if (ratio >= 0.375f) {
            return 5;
        } else if (ratio >= 0.25f) {
            return 6;
        } else {
            return 7;
        }
    }
    
    private int calculateParticleCount() {
        float size = this.getSize();
        float ratio = size / 1.0f;
        
        int baseCount = (int) (ratio * 32);
        return Math.min(baseCount, 32);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide && !hasPlayedSpawnSound) {
            hasPlayedSpawnSound = true;
            this.level().playSound(null, this.blockPosition(), 
                com.hollingsworth.arsnouveau.setup.registry.SoundRegistry.TEMPESTRY_FAMILY.get(), 
                SoundSource.NEUTRAL, 0.8f, 1.0f);
        }
    }
    
    @Override
    protected net.minecraft.core.particles.ParticleOptions getAmbientParticle() {
        return ParticleTypes.FALLING_WATER;
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
                    ParticleTypes.SPLASH,
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
                    ParticleTypes.BUBBLE_POP,
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

