package com.github.ars_zero.common.entity;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class BaseVoxelEntity extends Projectile implements GeoEntity {
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(BaseVoxelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(BaseVoxelEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BASE_SIZE = SynchedEntityData.defineId(BaseVoxelEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> FROZEN_UNTIL_TICK = SynchedEntityData.defineId(BaseVoxelEntity.class, EntityDataSerializers.LONG);
    
    protected int age = 0;
    protected SpellResolver resolver;
    
    public BaseVoxelEntity(EntityType<? extends BaseVoxelEntity> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
        this.setBaseSize(0.25f);
        this.setSize(0.25f);
        refreshDimensions();
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder pBuilder) {
        pBuilder.define(LIFETIME, 1200);
        pBuilder.define(SIZE, 0.25f);
        pBuilder.define(BASE_SIZE, 0.25f);
        pBuilder.define(FROZEN_UNTIL_TICK, 0L);
    }
    
    @Override
    public void tick() {
        super.tick();
        this.age++;
        
        if (!this.level().isClientSide && this.age >= this.getLifetime()) {
            resolveAndDiscard(null);
            return;
        }
        
        if (this.age % 10 == 0) {
            emitAmbientParticle();
        }
        
        if (this.entityData.get(FROZEN_UNTIL_TICK) >= this.level().getGameTime()) {
            return;
        }
        
        Vec3 thisPosition = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 nextPosition = thisPosition.add(motion);
        
        HitResult blockHitResult = this.level().clip(new net.minecraft.world.level.ClipContext(
            thisPosition, nextPosition, 
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE, 
            this
        ));
        
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            nextPosition = blockHitResult.getLocation();
        }
        
        EntityHitResult entityHitResult = this.findHitEntity(thisPosition, nextPosition);
        com.github.ars_zero.ArsZero.LOGGER.info("Tick collision check: blockHit={}, entityHit={}", blockHitResult.getType(), entityHitResult != null);
        
        HitResult hitResult = blockHitResult;
        if (entityHitResult != null) {
            hitResult = entityHitResult;
            com.github.ars_zero.ArsZero.LOGGER.info("Using entity hit result at {}", entityHitResult.getLocation());
        }
        
        if (hitResult.getType() != HitResult.Type.MISS && !net.neoforged.neoforge.event.EventHooks.onProjectileImpact(this, hitResult)) {
            com.github.ars_zero.ArsZero.LOGGER.info("Hit detected! Type: {}", hitResult.getType());
            this.onHit(hitResult);
            this.hasImpulse = true;
            return;
        }
        
        Vec3 deltaMovement = this.getDeltaMovement();
        this.setPos(this.getX() + deltaMovement.x, this.getY() + deltaMovement.y, this.getZ() + deltaMovement.z);
        
        this.setDeltaMovement(deltaMovement.scale(0.98));
        if (!this.isNoGravity()) {
            this.applyGravity();
        }
    }
    
    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }
    
    protected EntityHitResult findHitEntity(Vec3 startVec, Vec3 endVec) {
        return ProjectileUtil.getEntityHitResult(this.level(), this, startVec, endVec,
                this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), this::canHitEntity);
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        com.github.ars_zero.ArsZero.LOGGER.info("BaseVoxel onHitEntity called: {} at {}", result.getEntity().getName().getString(), result.getLocation());
        spawnHitParticles(result.getLocation());
        resolveAndDiscard(result);
    }
    
    @Override
    protected void onHitBlock(BlockHitResult result) {
        com.github.ars_zero.ArsZero.LOGGER.info("BaseVoxel onHitBlock called at {}", result.getLocation());
        spawnHitParticles(result.getLocation());
        resolveAndDiscard(result);
    }
    
    protected void resolveAndDiscard(HitResult hitResult) {
        if (!this.level().isClientSide && resolver != null && hitResult != null) {
            resolver.onResolveEffect(this.level(), hitResult);
        }
        
        if (!this.level().isClientSide && hitResult instanceof BlockHitResult blockHit) {
            onBlockCollision(blockHit);
        }
        
        this.discard();
    }
    
    protected void onBlockCollision(BlockHitResult blockHit) {
    }
    
    protected abstract void spawnHitParticles(Vec3 location);
    
    protected abstract net.minecraft.core.particles.ParticleOptions getAmbientParticle();
    
    private void emitAmbientParticle() {
        if (!this.level().isClientSide) {
            double size = this.getSize();
            double radius = size / 2.0;
            
            double theta = this.random.nextDouble() * 2 * Math.PI;
            double phi = this.random.nextDouble() * Math.PI;
            
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);
            
            ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(
                getAmbientParticle(),
                this.getX() + x,
                this.getY() + y,
                this.getZ() + z,
                1,
                0.0, 0.0, 0.0,
                0.0
            );
        }
    }
    
    public void setResolver(SpellResolver resolver) {
        this.resolver = resolver;
    }
    
    public SpellResolver getResolver() {
        return this.resolver;
    }
    
    public void setCaster(Entity caster) {
        this.setOwner(caster);
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.age = compound.getInt("age");
        this.setLifetime(compound.getInt("lifetime"));
        this.setSize(compound.getFloat("size"));
        this.setBaseSize(compound.getFloat("baseSize"));
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("age", this.age);
        compound.putInt("lifetime", this.getLifetime());
        compound.putFloat("size", this.getSize());
        compound.putFloat("baseSize", this.getBaseSize());
    }
    
    @Override
    public AABB getBoundingBoxForCulling() {
        double size = this.getSize();
        return new AABB(-size, -size, -size, size, size, size).move(this.position());
    }
    
    @Override
    public void refreshDimensions() {
        double size = this.getSize();
        double halfSize = size / 2.0;
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        this.setBoundingBox(new AABB(
            x - halfSize, y - halfSize, z - halfSize,
            x + halfSize, y + halfSize, z + halfSize
        ));
    }
    
    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        double size = this.getSize();
        double halfSize = size / 2.0;
        this.setBoundingBox(new AABB(
            x - halfSize, y - halfSize, z - halfSize,
            x + halfSize, y + halfSize, z + halfSize
        ));
    }
    
    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }
    
    public void setLifetime(int lifetime) {
        this.entityData.set(LIFETIME, lifetime);
    }
    
    public float getSize() {
        return this.entityData.get(SIZE);
    }
    
    public void setSize(float size) {
        this.entityData.set(SIZE, size);
        this.entityData.set(BASE_SIZE, size);
    }
    
    public float getBaseSize() {
        return this.entityData.get(BASE_SIZE);
    }
    
    public void setBaseSize(float baseSize) {
        this.entityData.set(BASE_SIZE, baseSize);
    }
    
    public int getAge() {
        return this.age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
    
    @Override
    public boolean isPickable() {
        return true;
    }
    
    @Override
    public boolean isPushable() {
        return true;
    }
    
    @Override
    public float getPickRadius() {
        return this.getSize();
    }
    
    public void freezePhysics() {
        this.entityData.set(FROZEN_UNTIL_TICK, this.level().getGameTime() + 1);
    }
    
    public boolean isPhysicsFrozen() {
        return this.entityData.get(FROZEN_UNTIL_TICK) >= this.level().getGameTime();
    }
    
    public abstract int getColor();
    public abstract boolean isEmissive();
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this, "controller", 0, this::predicate));
    }
    
    private software.bernie.geckolib.animation.PlayState predicate(software.bernie.geckolib.animation.AnimationState<BaseVoxelEntity> state) {
        if (this.age <= 7) {
            return state.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenPlay("spawn"));
        } else {
            return state.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("idle"));
        }
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

