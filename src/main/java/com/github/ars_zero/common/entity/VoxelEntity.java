package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class VoxelEntity extends Entity {
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(VoxelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(VoxelEntity.class, EntityDataSerializers.FLOAT);
    
    private int age = 0;
    
    public VoxelEntity(EntityType<? extends VoxelEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setSize(0.25f);
    }
    
    public VoxelEntity(Level level, double x, double y, double z, int lifetime) {
        this(ModEntities.VOXEL_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.setLifetime(lifetime);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder pBuilder) {
        pBuilder.define(LIFETIME, 200);
        pBuilder.define(SIZE, 0.25f);
    }
    
    @Override
    public void tick() {
        super.tick();
        this.age++;
        
        if (this.age >= this.getLifetime()) {
            this.discard();
        }
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.age = compound.getInt("age");
        this.setLifetime(compound.getInt("lifetime"));
        this.setSize(compound.getFloat("size"));
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("age", this.age);
        compound.putInt("lifetime", this.getLifetime());
        compound.putFloat("size", this.getSize());
    }
    
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity p_352287_) {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this, p_352287_, 0);
    }
    
    @Override
    public AABB getBoundingBoxForCulling() {
        float size = this.getSize();
        return new AABB(-size/2, -size/2, -size/2, size/2, size/2, size/2).move(this.position());
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
    }
    
    public int getAge() {
        return this.age;
    }
    
    @Override
    public boolean isPickable() {
        return false;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    public boolean isNoGravity() {
        return true;
    }
}
