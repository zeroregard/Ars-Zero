package com.github.ars_zero.common.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.registry.ModEntities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GrappleTetherEntity extends Entity {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static final EntityDataAccessor<Float> MAX_LENGTH = SynchedEntityData.defineId(GrappleTetherEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(GrappleTetherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(GrappleTetherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockPos> TARGET_POS = SynchedEntityData.defineId(GrappleTetherEntity.class, EntityDataSerializers.BLOCK_POS);
    
    private static final float DEFAULT_MAX_LENGTH = 10.0f;
    private static final double SPRING_CONSTANT = 0.05;
    private static final double DAMPING = 0.9;
    
    @Nullable
    private UUID playerUUID;
    @Nullable
    private UUID targetEntityUUID;
    private Vec3 targetPosVec;
    private boolean isBlockTarget;
    private int age = 0;
    
    public GrappleTetherEntity(EntityType<? extends GrappleTetherEntity> entityType, Level level) {
        super(entityType, level);
        this.setMaxLength(DEFAULT_MAX_LENGTH);
        this.setLifetime(20);
        this.setAge(0);
    }
    
    public GrappleTetherEntity(Level level, BlockPos targetPos, Player player, float maxLength, int lifetime) {
        this(ModEntities.GRAPPLE_TETHER.get(), level);
        this.targetPosVec = Vec3.atCenterOf(targetPos);
        this.getEntityData().set(TARGET_POS, targetPos);
        this.playerUUID = player.getUUID();
        this.targetEntityUUID = null;
        this.isBlockTarget = true;
        this.setMaxLength(maxLength);
        this.setLifetime(lifetime);
        this.setPos(targetPosVec.x, targetPosVec.y, targetPosVec.z);
    }
    
    public GrappleTetherEntity(Level level, Entity targetEntity, Player player, float maxLength, int lifetime) {
        this(ModEntities.GRAPPLE_TETHER.get(), level);
        Vec3 targetPos = targetEntity.position();
        BlockPos targetBlockPos = BlockPos.containing(targetPos);
        this.targetPosVec = targetPos;
        this.getEntityData().set(TARGET_POS, targetBlockPos);
        this.playerUUID = player.getUUID();
        this.targetEntityUUID = targetEntity.getUUID();
        this.isBlockTarget = false;
        this.setMaxLength(maxLength);
        this.setLifetime(lifetime);
        this.setPos(targetPosVec.x, targetPosVec.y, targetPosVec.z);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder pBuilder) {
        pBuilder.define(MAX_LENGTH, DEFAULT_MAX_LENGTH);
        pBuilder.define(LIFETIME, 20);
        pBuilder.define(AGE, 0);
        pBuilder.define(TARGET_POS, BlockPos.ZERO);
        // Note: UUIDs can't be directly synced via SynchedEntityData
        // We'll use NBT serialization via addAdditionalSaveData/readAdditionalSaveData
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (this.level().isClientSide) {
            return;
        }
        
        this.age++;
        this.setAge(this.age);
        
        if (this.age == 1) {
            LOGGER.info("[Tether] Entity {} first tick: age={}, lifetime={}, playerUUID={}, targetEntityUUID={}, isBlockTarget={}", 
                this.getUUID(), this.age, this.getLifetime(), this.playerUUID, this.targetEntityUUID, this.isBlockTarget);
        }
        
        if (this.age % 20 == 0) {
            LOGGER.info("[Tether] Entity {} tick: age={}/{}, playerUUID={}, targetEntityUUID={}", 
                this.getUUID(), this.age, this.getLifetime(), this.playerUUID, this.targetEntityUUID);
        }
        
        if (this.playerUUID == null) {
            LOGGER.warn("[Tether] Entity {} discarded: no playerUUID", this.getUUID());
            this.discard();
            return;
        }
        
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        Player player = serverLevel.getPlayerByUUID(this.playerUUID);
        if (player == null || !player.isAlive()) {
            LOGGER.warn("[Tether] Entity {} discarded: player null or not alive (player={})", 
                this.getUUID(), player != null ? player.getName().getString() : "null");
            this.discard();
            return;
        }
        
        if (this.age >= this.getLifetime()) {
            LOGGER.info("[Tether] Entity {} discarded: lifetime expired (age={} >= lifetime={})", 
                this.getUUID(), this.age, this.getLifetime());
            this.discard();
            return;
        }
        
        Vec3 targetPos = this.getTargetPosition(serverLevel);
        if (targetPos == null) {
            LOGGER.warn("[Tether] Entity {} discarded: target position is null (isBlockTarget={}, targetEntityUUID={})", 
                this.getUUID(), this.isBlockTarget, this.targetEntityUUID);
            this.discard();
            return;
        }
        
        this.targetPosVec = targetPos;
        this.applyPhysics(player, targetPos);
    }
    
    @Nullable
    private Vec3 getTargetPosition(ServerLevel serverLevel) {
        if (this.isBlockTarget) {
            BlockPos targetPos = this.getEntityData().get(TARGET_POS);
            if (targetPos != null && !targetPos.equals(BlockPos.ZERO)) {
                return Vec3.atCenterOf(targetPos);
            }
            return null;
        } else if (this.targetEntityUUID != null) {
            Entity targetEntity = serverLevel.getEntity(this.targetEntityUUID);
            if (targetEntity != null && targetEntity.isAlive()) {
                return targetEntity.position();
            }
            return null;
        }
        return null;
    }
    
    private void applyPhysics(Player player, Vec3 targetPos) {
        Vec3 playerPos = player.position().add(0, player.getEyeHeight() * 0.5, 0);
        
        Vec3 toPlayer = playerPos.subtract(targetPos);
        double currentLength = toPlayer.length();
        float maxLength = this.getMaxLength();
        
        if (currentLength > maxLength) {
            Vec3 direction = toPlayer.normalize();
            Vec3 desiredPos = targetPos.add(direction.scale(maxLength));
            
            Vec3 pullForce = desiredPos.subtract(playerPos);
            Vec3 currentVelocity = player.getDeltaMovement();
            
            Vec3 springForce = pullForce.scale(SPRING_CONSTANT);
            Vec3 dampedVelocity = currentVelocity.scale(DAMPING);
            
            Vec3 newVelocity = springForce.add(dampedVelocity);
            player.setDeltaMovement(newVelocity);
            player.hurtMarked = true;
        }
    }
    
    public void setMaxLength(float maxLength) {
        this.getEntityData().set(MAX_LENGTH, Math.max(1.0f, Math.min(50.0f, maxLength)));
    }
    
    public float getMaxLength() {
        return this.getEntityData().get(MAX_LENGTH);
    }
    
    public void setLifetime(int lifetime) {
        this.getEntityData().set(LIFETIME, Math.max(1, lifetime));
    }
    
    public int getLifetime() {
        return this.getEntityData().get(LIFETIME);
    }
    
    public void extendLifetime(int additionalTicks) {
        int newLifetime = this.getLifetime() + additionalTicks;
        this.setLifetime(newLifetime);
    }
    
    public int getAge() {
        return this.getEntityData().get(AGE);
    }
    
    public void setAge(int age) {
        this.getEntityData().set(AGE, age);
    }
    
    public int getRemainingTicks() {
        return Math.max(0, this.getLifetime() - this.age);
    }
    
    @Nullable
    public UUID getPlayerUUID() {
        return this.playerUUID;
    }
    
    @Nullable
    public UUID getTargetEntityUUID() {
        return this.targetEntityUUID;
    }
    
    @Nullable
    public BlockPos getTargetPos() {
        return this.getEntityData().get(TARGET_POS);
    }
    
    public boolean isBlockTarget() {
        return this.isBlockTarget;
    }
    
    public boolean isTarget(BlockPos pos) {
        if (!this.isBlockTarget) {
            return false;
        }
        BlockPos targetPos = this.getTargetPos();
        return targetPos != null && targetPos.equals(pos);
    }
    
    public boolean isTarget(Entity entity) {
        if (this.isBlockTarget || this.targetEntityUUID == null) {
            return false;
        }
        return this.targetEntityUUID.equals(entity.getUUID());
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("PlayerUUID")) {
            this.playerUUID = compound.getUUID("PlayerUUID");
        }
        if (compound.contains("TargetEntityUUID")) {
            this.targetEntityUUID = compound.getUUID("TargetEntityUUID");
        }
        this.isBlockTarget = compound.getBoolean("IsBlockTarget");
        if (compound.contains("TargetPos")) {
            BlockPos targetPos = BlockPos.of(compound.getLong("TargetPos"));
            this.getEntityData().set(TARGET_POS, targetPos);
            if (this.isBlockTarget) {
                this.targetPosVec = Vec3.atCenterOf(targetPos);
            }
        }
        this.age = compound.getInt("Age");
        this.setAge(this.age);
        
        // Sync playerUUID to client via NBT
        if (this.level().isClientSide && compound.contains("PlayerUUID")) {
            this.playerUUID = compound.getUUID("PlayerUUID");
        }
        this.setMaxLength(compound.getFloat("MaxLength"));
        this.setLifetime(compound.getInt("Lifetime"));
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (this.playerUUID != null) {
            compound.putUUID("PlayerUUID", this.playerUUID);
        }
        if (this.targetEntityUUID != null) {
            compound.putUUID("TargetEntityUUID", this.targetEntityUUID);
        }
        compound.putBoolean("IsBlockTarget", this.isBlockTarget);
        BlockPos targetPos = this.getEntityData().get(TARGET_POS);
        if (targetPos != null) {
            compound.putLong("TargetPos", targetPos.asLong());
        }
        compound.putInt("Age", this.age);
        compound.putFloat("MaxLength", this.getMaxLength());
        compound.putInt("Lifetime", this.getLifetime());
    }
    
    @Override
    public boolean isPickable() {
        return false;
    }
    
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }
    
    @Override
    public boolean isInvisible() {
        return false;
    }
    
    @Override
    public boolean shouldRender(double pX, double pY, double pZ) {
        return true;
    }
}
