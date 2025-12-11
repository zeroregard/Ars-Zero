package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ArcaneShieldEntity extends Entity implements GeoEntity {
    
    private static final EntityDataAccessor<Boolean> REFLECTIVE_MODE = SynchedEntityData.defineId(ArcaneShieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> HEALTH_MULTIPLIER = SynchedEntityData.defineId(ArcaneShieldEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURRENT_HEALTH = SynchedEntityData.defineId(ArcaneShieldEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(ArcaneShieldEntity.class, EntityDataSerializers.INT);
    
    private static final float DEFAULT_HEALTH = 20.0f;
    private static final int DEFAULT_LIFETIME_TICKS = 1200;
    private static final float WIDTH = 1.0f;
    private static final float HEIGHT = 1.0f;
    private static final float DEPTH = 0.25f;
    
    private boolean hasRotated = false;
    private int age = 0;
    @Nullable
    private UUID ownerUUID;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public ArcaneShieldEntity(EntityType<? extends ArcaneShieldEntity> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
        this.setNoGravity(true);
        this.setHealthMultiplier(1.0f);
        this.setCurrentHealth(getMaxHealth());
        this.setLifetime(DEFAULT_LIFETIME_TICKS);
    }
    
    public ArcaneShieldEntity(Level level, double x, double y, double z, float healthMultiplier, boolean reflectiveMode, int lifetime) {
        this(ModEntities.ARCANE_SHIELD_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.setHealthMultiplier(healthMultiplier);
        this.setReflectiveMode(reflectiveMode);
        this.setCurrentHealth(getMaxHealth());
        this.setLifetime(lifetime);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(REFLECTIVE_MODE, false);
        builder.define(HEALTH_MULTIPLIER, 1.0f);
        builder.define(CURRENT_HEALTH, DEFAULT_HEALTH);
        builder.define(LIFETIME, DEFAULT_LIFETIME_TICKS);
    }
    
    @Override
    public void tick() {
        super.tick();
        this.age++;
        
        if (!hasRotated && this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel) {
            Entity owner = serverLevel.getEntity(this.ownerUUID);
            if (owner != null) {
                Vec3 casterPos = owner.position();
                Vec3 shieldPos = this.position();
                Vec3 direction = shieldPos.subtract(casterPos);
                
                if (direction.lengthSqr() > 0.01) {
                    direction = direction.normalize();
                    double yaw = Math.atan2(-direction.x, direction.z);
                    double pitch = -Math.asin(direction.y);
                    this.setYRot((float) Math.toDegrees(yaw));
                    this.setXRot((float) Math.toDegrees(pitch));
                }
                hasRotated = true;
            }
        }
        
        if (!this.level().isClientSide) {
            if (this.age >= this.getLifetime()) {
                this.discard();
                return;
            }
            
            if (this.getCurrentHealth() <= 0) {
                this.discard();
            }
        }
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.age = compound.getInt("age");
        this.setReflectiveMode(compound.getBoolean("reflectiveMode"));
        this.setHealthMultiplier(compound.getFloat("healthMultiplier"));
        this.setCurrentHealth(compound.getFloat("currentHealth"));
        this.setLifetime(compound.getInt("lifetime"));
        this.hasRotated = compound.getBoolean("hasRotated");
        if (compound.hasUUID("ownerUUID")) {
            this.ownerUUID = compound.getUUID("ownerUUID");
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("age", this.age);
        compound.putBoolean("reflectiveMode", this.isReflectiveMode());
        compound.putFloat("healthMultiplier", this.getHealthMultiplier());
        compound.putFloat("currentHealth", this.getCurrentHealth());
        compound.putInt("lifetime", this.getLifetime());
        compound.putBoolean("hasRotated", this.hasRotated);
        if (this.ownerUUID != null) {
            compound.putUUID("ownerUUID", this.ownerUUID);
        }
    }
    
    @Override
    public boolean canBeHitByProjectile() {
        return true;
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        
        Entity attacker = source.getDirectEntity();
        boolean isProjectile = attacker instanceof Projectile;
        boolean isMelee = attacker instanceof LivingEntity && !isProjectile;
        
        if (!isProjectile && !isMelee) {
            return false;
        }
        
        Vec3 attackDirection = null;
        if (attacker != null) {
            Vec3 attackerPos = attacker.position();
            Vec3 shieldPos = this.position();
            attackDirection = attackerPos.subtract(shieldPos).normalize();
        }
        
        boolean hitFromFront = false;
        if (attackDirection != null) {
            Vec3 forward = this.getForward();
            double dot = attackDirection.dot(forward);
            hitFromFront = dot < -0.5;
        }
        
        float damageMultiplier;
        if (isReflectiveMode()) {
            damageMultiplier = isProjectile ? 1.0f : 2.0f;
        } else {
            damageMultiplier = isProjectile ? 2.0f : 1.0f;
        }
        
        float actualDamage = amount * damageMultiplier;
        this.setCurrentHealth(this.getCurrentHealth() - actualDamage);
        
        if (!this.level().isClientSide) {
            Vec3 location = this.position();
            this.level().playSound(null, location.x, location.y, location.z,
                SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.6f, 1.0f + this.random.nextFloat() * 0.3f);
            
            triggerAnim("controller", "damage");
        }
        
        if (isReflectiveMode() && hitFromFront) {
            if (isProjectile && attacker instanceof Projectile projectile) {
                Vec3 forward = this.getForward();
                Vec3 incoming = projectile.getDeltaMovement().normalize();
                double dot = incoming.dot(forward);
                Vec3 reflected = incoming.subtract(forward.scale(2.0 * dot));
                double speed = projectile.getDeltaMovement().length();
                projectile.setDeltaMovement(reflected.normalize().scale(speed));
                
                double yaw = Math.atan2(-reflected.x, reflected.z);
                double pitch = -Math.asin(reflected.y);
                projectile.setYRot((float) Math.toDegrees(yaw));
                projectile.setXRot((float) Math.toDegrees(pitch));
                return true;
            } else if (isMelee && attacker instanceof LivingEntity living) {
                Vec3 forward = this.getForward();
                Vec3 knockback = forward.scale(0.4);
                living.setDeltaMovement(living.getDeltaMovement().add(knockback));
                living.hurtMarked = true;
                return true;
            }
        }
        
        if (isProjectile && attacker instanceof Projectile) {
            attacker.discard();
        }
        
        return true;
    }
    
    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        if (reason == net.minecraft.world.entity.Entity.RemovalReason.DISCARDED && !this.level().isClientSide) {
            Vec3 location = this.position();
            this.level().playSound(null, location.x, location.y, location.z,
                SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.8f, 1.0f + this.random.nextFloat() * 0.2f);
            
            if (this.level() instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 20; i++) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
                    double offsetY = (this.random.nextDouble() - 0.5) * 0.5;
                    double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;
                    serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState()),
                        location.x + offsetX,
                        location.y + offsetY,
                        location.z + offsetZ,
                        1,
                        0.0, 0.0, 0.0,
                        0.05
                    );
                }
            }
        }
        super.remove(reason);
    }
    
    @Override
    public void refreshDimensions() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        this.setBoundingBox(new AABB(
            x - WIDTH / 2.0, y - HEIGHT / 2.0, z - DEPTH / 2.0,
            x + WIDTH / 2.0, y + HEIGHT / 2.0, z + DEPTH / 2.0
        ));
    }
    
    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        this.refreshDimensions();
    }
    
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox();
    }
    
    @Override
    public boolean isPickable() {
        return true;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    public Vec3 getForward() {
        double yawRad = Math.toRadians(this.getYRot());
        double pitchRad = Math.toRadians(this.getXRot());
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        return new Vec3(sinYaw * cosPitch, -sinPitch, -cosYaw * cosPitch);
    }
    
    public boolean isReflectiveMode() {
        return this.entityData.get(REFLECTIVE_MODE);
    }
    
    public void setReflectiveMode(boolean reflective) {
        this.entityData.set(REFLECTIVE_MODE, reflective);
    }
    
    public float getHealthMultiplier() {
        return this.entityData.get(HEALTH_MULTIPLIER);
    }
    
    public void setHealthMultiplier(float multiplier) {
        this.entityData.set(HEALTH_MULTIPLIER, multiplier);
    }
    
    public float getMaxHealth() {
        return DEFAULT_HEALTH * this.getHealthMultiplier();
    }
    
    public float getCurrentHealth() {
        return this.entityData.get(CURRENT_HEALTH);
    }
    
    public void setCurrentHealth(float health) {
        this.entityData.set(CURRENT_HEALTH, Math.max(0.0f, Math.min(health, getMaxHealth())));
    }
    
    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }
    
    public void setLifetime(int lifetime) {
        this.entityData.set(LIFETIME, lifetime);
    }
    
    public int getAge() {
        return this.age;
    }
    
    public void setOwner(@Nullable LivingEntity owner) {
        this.ownerUUID = owner != null ? owner.getUUID() : null;
    }
    
    @Nullable
    public LivingEntity getOwner() {
        if (this.ownerUUID == null) {
            return null;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.ownerUUID);
            return entity instanceof LivingEntity living ? living : null;
        }
        return null;
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    
    private PlayState predicate(AnimationState<ArcaneShieldEntity> state) {
        return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
