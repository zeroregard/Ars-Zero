package com.github.ars_zero.common.entity;

import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.util.ManaUtil;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.setup.config.ServerConfig;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AbstractChargerEntity extends Entity implements ILifespanExtendable {
    private static final EntityDataAccessor<Integer> DATA_LIFESPAN = SynchedEntityData.defineId(AbstractChargerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX_LIFESPAN = SynchedEntityData.defineId(AbstractChargerEntity.class, EntityDataSerializers.INT);
    
    private static final String TAG_CASTER_UUID = "caster_uuid";
    private static final String TAG_LIFESPAN = "lifespan";
    private static final String TAG_MAX_LIFESPAN = "max_lifespan";
    
    protected UUID casterUUID;
    protected int lifespan;
    protected int maxLifespan;
    protected int tickCount = 0;

    public AbstractChargerEntity(EntityType<? extends AbstractChargerEntity> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
        this.setNoGravity(true);
        this.setBoundingBox(new AABB(0, 0, 0, 0, 0, 0));
    }

    public void setCasterUUID(UUID uuid) {
        this.casterUUID = uuid;
    }

    public void setLifespan(int ticks) {
        this.maxLifespan = Math.max(0, ticks);
        this.lifespan = this.maxLifespan;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_LIFESPAN, this.lifespan);
            this.entityData.set(DATA_MAX_LIFESPAN, this.maxLifespan);
        }
    }

    public int getLifespan() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_LIFESPAN);
        }
        return this.lifespan;
    }

    public int getMaxLifespan() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_MAX_LIFESPAN);
        }
        return this.maxLifespan;
    }

    @Override
    public void tick() {
        super.tick();
        
        if (this.level().isClientSide) {
            return;
        }
        
        ServerLevel serverLevel = (ServerLevel) this.level();
        tickCount++;
        
        if (lifespan > 0) {
            this.lifespan--;
            this.entityData.set(DATA_LIFESPAN, this.lifespan);
            
            if (casterUUID != null) {
                LivingEntity caster = serverLevel.getEntity(casterUUID) instanceof LivingEntity living ? living : null;
                if (caster != null && caster instanceof Player player) {
                    IManaCap manaCap = CapabilityRegistry.getMana(caster);
                    if (manaCap != null && manaCap.getCurrentMana() > 0) {
                        if (canTransferMana()) {
                            double manaRegen = ManaUtil.getManaRegen(player);
                            int regenInterval = ServerConfig.REGEN_INTERVAL.get();
                            double manaRegenPerTick = manaRegen / Math.max(1, regenInterval);
                            double manaToDrain = manaRegenPerTick * 1.1;
                            int manaToTransfer = (int) Math.floor(manaToDrain);
                            
                            if (manaToTransfer > 0) {
                                double actualManaToDrain = Math.min(manaToDrain, manaCap.getCurrentMana());
                                int actualManaToTransfer = (int) Math.floor(actualManaToDrain);
                                
                                if (actualManaToTransfer > 0 && transferMana(serverLevel, actualManaToTransfer)) {
                                    manaCap.removeMana(actualManaToDrain);
                                    spawnChargeParticles(serverLevel, getParticlePosition(), tickCount, manaRegen);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            this.discard();
        }
    }

    protected abstract boolean canTransferMana();
    protected abstract boolean transferMana(ServerLevel level, int manaAmount);
    protected abstract Vec3 getParticlePosition();
    protected abstract void spawnChargeParticles(ServerLevel level, Vec3 position, int tickCount, double manaRegen);

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(DATA_LIFESPAN, 0);
        builder.define(DATA_MAX_LIFESPAN, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains(TAG_CASTER_UUID)) {
            this.casterUUID = compound.getUUID(TAG_CASTER_UUID);
        }
        if (compound.contains(TAG_LIFESPAN)) {
            this.lifespan = compound.getInt(TAG_LIFESPAN);
        }
        if (compound.contains(TAG_MAX_LIFESPAN)) {
            this.maxLifespan = compound.getInt(TAG_MAX_LIFESPAN);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (casterUUID != null) {
            compound.putUUID(TAG_CASTER_UUID, casterUUID);
        }
        compound.putInt(TAG_LIFESPAN, lifespan);
        compound.putInt(TAG_MAX_LIFESPAN, maxLifespan);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public float getPickRadius() {
        return 0.0f;
    }

    @Override
    public boolean isInvisible() {
        return true;
    }

    @Override
    public boolean isInvisibleTo(Player player) {
        return true;
    }

    @Override
    public void addLifespan(LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        int extensionAmount = 1;
        
        if (spellStats != null) {
            int extendTimeLevel = spellStats.getBuffCount(AugmentExtendTime.INSTANCE);
            extensionAmount += extendTimeLevel;
        }
        
        this.lifespan += extensionAmount;
        this.maxLifespan = Math.max(this.maxLifespan, this.lifespan);
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_LIFESPAN, this.lifespan);
            this.entityData.set(DATA_MAX_LIFESPAN, this.maxLifespan);
        }
    }
}

