package com.github.ars_zero.common.entity;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class AbstractConvergenceEntity extends Entity implements ILifespanExtendable, GeoEntity {
    private int lifespan;
    private int maxLifespan;

    public AbstractConvergenceEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
        this.setNoGravity(true);
        this.setBoundingBox(new AABB(0, 0, 0, 0, 0, 0));
    }

    public void setLifespan(int ticks) {
        this.maxLifespan = Math.max(0, ticks);
        this.lifespan = this.maxLifespan;
    }

    public int getLifespan() {
        return this.lifespan;
    }

    public int getMaxLifespan() {
        return this.maxLifespan;
    }

    public boolean shouldStart() {
        return this.lifespan <= 0;
    }

    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide) {
            if (this.lifespan > 0) {
                this.lifespan--;
            } else if (shouldStart()) {
                onLifespanReached();
            }
        }
    }

    protected abstract void onLifespanReached();

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("lifespan")) {
            this.lifespan = compound.getInt("lifespan");
        }
        if (compound.contains("max_lifespan")) {
            this.maxLifespan = compound.getInt("max_lifespan");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("lifespan", this.lifespan);
        compound.putInt("max_lifespan", this.maxLifespan);
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
        this.lifespan++;
        this.maxLifespan = Math.max(this.maxLifespan, this.lifespan);
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

