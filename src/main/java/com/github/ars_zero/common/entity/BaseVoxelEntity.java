package com.github.ars_zero.common.entity;

import com.github.ars_zero.common.entity.interaction.VoxelInteraction;
import com.github.ars_zero.common.entity.interaction.VoxelInteractionRegistry;
import com.github.ars_zero.common.entity.interaction.VoxelInteractionResult;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class BaseVoxelEntity extends Projectile implements GeoEntity {
    public static final int DEFAULT_LIFETIME_TICKS = 1200;
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(BaseVoxelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(BaseVoxelEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BASE_SIZE = SynchedEntityData.defineId(BaseVoxelEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> FROZEN_UNTIL_TICK = SynchedEntityData.defineId(BaseVoxelEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> PICKABLE = SynchedEntityData.defineId(BaseVoxelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SPAWNER_OWNED = SynchedEntityData.defineId(BaseVoxelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NO_GRAVITY_CUSTOM = SynchedEntityData.defineId(BaseVoxelEntity.class, EntityDataSerializers.BOOLEAN);
    
    protected int age = 0;
    protected SpellResolver resolver;
    
    public BaseVoxelEntity(EntityType<? extends BaseVoxelEntity> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
        this.setBaseSize(0.25f);
        this.setSize(0.25f);
        refreshDimensions();
    }
    
    @Nullable
    protected abstract SoundEvent getSpawnSound();
    
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder pBuilder) {
        pBuilder.define(LIFETIME, DEFAULT_LIFETIME_TICKS);
        pBuilder.define(SIZE, 0.25f);
        pBuilder.define(BASE_SIZE, 0.25f);
        pBuilder.define(FROZEN_UNTIL_TICK, 0L);
        pBuilder.define(PICKABLE, true);
        pBuilder.define(SPAWNER_OWNED, false);
        pBuilder.define(NO_GRAVITY_CUSTOM, false);
    }
    
    @Override
    public void tick() {
        super.tick();
        this.age++;
        
        if (!this.level().isClientSide && this.age == 1) {
            SoundEvent spawnSound = getSpawnSound();
            if (spawnSound != null) {
                this.level().playSound(null, this.blockPosition(), 
                    spawnSound, SoundSource.NEUTRAL, 0.8f, 1.0f);
            }
        }
        
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
        
        HitResult hitResult = blockHitResult;
        if (entityHitResult != null) {
            hitResult = entityHitResult;
        }
        
        if (hitResult.getType() != HitResult.Type.MISS && !net.neoforged.neoforge.event.EventHooks.onProjectileImpact(this, hitResult)) {
            this.onHit(hitResult);
            this.hasImpulse = true;
            return;
        }
        
        Vec3 deltaMovement = this.getDeltaMovement();
        this.setPos(this.getX() + deltaMovement.x, this.getY() + deltaMovement.y, this.getZ() + deltaMovement.z);
        
        if (!this.entityData.get(NO_GRAVITY_CUSTOM)) {
            this.applyGravity();
        }
    }
    
    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }
    
    protected EntityHitResult findHitEntity(Vec3 startVec, Vec3 endVec) {
        double inflateAmount = Math.max(0.1, this.getSize() * 0.5);
        return ProjectileUtil.getEntityHitResult(this.level(), this, startVec, endVec,
                this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(inflateAmount), this::canHitEntity);
    }
    
    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity instanceof BaseVoxelEntity) {
            return true;
        }
        return super.canHitEntity(entity);
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hitEntity = result.getEntity();
        
        if (hitEntity instanceof BaseVoxelEntity otherVoxel) {
            VoxelInteraction interaction = VoxelInteractionRegistry.getInteraction(this, otherVoxel);
            
            if (interaction != null && interaction.shouldInteract(this, otherVoxel)) {
                VoxelInteractionResult interactionResult = interaction.interact(this, otherVoxel);
                applyInteractionResult(interactionResult, otherVoxel);
                return;
            }
        }
        
        spawnHitParticles(result.getLocation());
        resolveAndDiscard(result);
    }
    
    @Override
    protected void onHitBlock(BlockHitResult result) {
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
        this.setPickable(compound.getBoolean("pickable"));
        if (compound.contains("NoGravityCustom")) {
            this.setNoGravityCustom(compound.getBoolean("NoGravityCustom"));
        }
        if (compound.contains("spawnerOwned")) {
            this.setSpawnerOwned(compound.getBoolean("spawnerOwned"));
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("age", this.age);
        compound.putInt("lifetime", this.getLifetime());
        compound.putFloat("size", this.getSize());
        compound.putFloat("baseSize", this.getBaseSize());
        compound.putBoolean("pickable", this.entityData.get(PICKABLE));
        compound.putBoolean("NoGravityCustom", this.getNoGravityCustom());
        compound.putBoolean("spawnerOwned", this.entityData.get(SPAWNER_OWNED));
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
    
    protected void applyInteractionResult(VoxelInteractionResult result, BaseVoxelEntity other) {
        Vec3 location = result.getInteractionLocation();
        
        result.getParticleType().ifPresent(particleType -> {
            if (!this.level().isClientSide) {
                for (int i = 0; i < result.getParticleCount(); i++) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
                    double offsetY = (this.random.nextDouble() - 0.5) * 0.5;
                    double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;
                    ((ServerLevel) this.level()).sendParticles(
                        particleType,
                        location.x + offsetX,
                        location.y + offsetY,
                        location.z + offsetZ,
                        1,
                        0.0, 0.1, 0.0,
                        0.02
                    );
                }
            }
        });
        
        result.getSoundEvent().ifPresent(sound -> {
            if (!this.level().isClientSide) {
                this.level().playSound(null, location.x, location.y, location.z, 
                    sound, this.getSoundSource(), 1.0f, 1.0f);
            }
        });
        
        VoxelInteractionResult.ActionType primaryAction = result.getPrimaryAction();
        VoxelInteractionResult.ActionType secondaryAction = result.getSecondaryAction();
        
        switch (primaryAction) {
            case DISCARD:
                this.setSpawnerOwned(false);
                this.discard();
                break;
            case RESOLVE:
                this.setSpawnerOwned(false);
                EntityHitResult fakeHit = new EntityHitResult(other, location);
                this.resolveAndDiscard(fakeHit);
                break;
            case RESIZE:
                float newSize = result.getPrimaryNewSize();
                if (newSize < 0.0625f) {
                    this.setSpawnerOwned(false);
                    this.discard();
                } else {
                    this.setSize(newSize);
                    this.refreshDimensions();
                }
                break;
            case REPEL:
                Vec3 repelDir = result.getPrimaryRepelDirection();
                if (repelDir != null) {
                    this.setDeltaMovement(repelDir.scale(result.getRepelForce()));
                }
                break;
            case CONTINUE:
                break;
        }
        
        switch (secondaryAction) {
            case DISCARD:
                other.setSpawnerOwned(false);
                other.discard();
                break;
            case RESOLVE:
                other.setSpawnerOwned(false);
                EntityHitResult fakeHit = new EntityHitResult(this, location);
                other.resolveAndDiscard(fakeHit);
                break;
            case RESIZE:
                float newSize = result.getSecondaryNewSize();
                if (newSize < 0.0625f) {
                    other.setSpawnerOwned(false);
                    other.discard();
                } else {
                    other.setSize(newSize);
                    other.refreshDimensions();
                }
                break;
            case REPEL:
                Vec3 repelDir = result.getSecondaryRepelDirection();
                if (repelDir != null) {
                    other.setDeltaMovement(repelDir.scale(result.getRepelForce()));
                }
                break;
            case CONTINUE:
                break;
        }
    }
    
    public int getAge() {
        return this.age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
    
    @Override
    public boolean isPickable() {
        return this.entityData.get(PICKABLE);
    }
    
    public void setPickable(boolean pickable) {
        this.entityData.set(PICKABLE, pickable);
    }
    
    public boolean isSpawnerOwned() {
        return this.entityData.get(SPAWNER_OWNED);
    }
    
    public void setSpawnerOwned(boolean owned) {
        this.entityData.set(SPAWNER_OWNED, owned);
    }
    
    public void setNoGravityCustom(boolean noGravity) {
        this.entityData.set(NO_GRAVITY_CUSTOM, noGravity);
        super.setNoGravity(noGravity);
    }
    
    public boolean getNoGravityCustom() {
        return this.entityData.get(NO_GRAVITY_CUSTOM);
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
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    
    private PlayState predicate(AnimationState<BaseVoxelEntity> state) {
        if (this.age <= 7) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("spawn"));
        } else {
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

