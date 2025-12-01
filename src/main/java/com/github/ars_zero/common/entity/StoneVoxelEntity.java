package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class StoneVoxelEntity extends BaseVoxelEntity {
    
    private static final int COLOR = 0x8A7F74;
    private static final double DAMAGE_SPEED_THRESHOLD = 0.35;
    private static final float BASE_DAMAGE = 1.5f;
    private static final float DAMAGE_SCALE = 2.0f;
    private static final float MAX_DAMAGE = 6.0f;
    
    public StoneVoxelEntity(EntityType<? extends StoneVoxelEntity> entityType, Level level) {
        super(entityType, level);
    }
    
    public StoneVoxelEntity(Level level, double x, double y, double z, int lifetime) {
        this(ModEntities.STONE_VOXEL_ENTITY.get(), level);
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
        spawnHitParticles(blockHit.getLocation());
        this.discard();
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hit = result.getEntity();
        if (hit instanceof BaseVoxelEntity) {
            super.onHitEntity(result);
            return;
        }
        if (!this.level().isClientSide) {
            if (hit instanceof LivingEntity living) {
                applyImpactDamage(living);
            }
            spawnHitParticles(result.getLocation());
        }
        this.discard();
    }
    
    private void applyImpactDamage(LivingEntity target) {
        double speed = this.getDeltaMovement().length();
        if (speed < DAMAGE_SPEED_THRESHOLD) {
            return;
        }
        float sizeScale = Math.max(1.0f, this.getSize() / BaseVoxelEntity.DEFAULT_BASE_SIZE);
        float damage = BASE_DAMAGE + (float) ((speed - DAMAGE_SPEED_THRESHOLD) * DAMAGE_SCALE);
        damage *= sizeScale;
        damage = Math.min(damage, MAX_DAMAGE);
        Entity owner = this.getOwner();
        LivingEntity sender = owner instanceof LivingEntity ? (LivingEntity) owner : null;
        target.hurt(this.level().damageSources().thrown(this, sender), damage);
        Vec3 impulse = this.getDeltaMovement().scale(0.35);
        target.push(impulse.x, Math.max(0.1, impulse.y + 0.15), impulse.z);
        target.hurtMarked = true;
    }
    
    @Override
    protected ParticleOptions getAmbientParticle() {
        return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
    }
    
    @Override
    protected void spawnHitParticles(Vec3 location) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ParticleOptions option = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        int particleCount = Math.max(8, (int) (this.getSize() / BaseVoxelEntity.DEFAULT_BASE_SIZE) * 6);
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 0.4;
            double offsetY = (this.random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.4;
            serverLevel.sendParticles(option, location.x + offsetX, location.y + offsetY, location.z + offsetZ, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
