package com.github.ars_zero.common.entity;

import com.github.ars_zero.common.config.ServerConfig;
import com.github.ars_zero.common.explosion.LargeExplosionDamage;
import com.github.ars_zero.common.explosion.LargeExplosionPrecompute;
import com.github.ars_zero.common.explosion.ExplosionWorkList;
import com.github.ars_zero.common.util.BlockImmutabilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ExplosionControllerEntity extends Entity {
    private static final int SILENT_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;

    private boolean active;
    private double radius;
    private float baseDamage;
    private float powerMultiplier;

    private ExplosionWorkList workList;
    private int nextWorkIndex;

    private long[] deferredPositions;
    private int deferredSize;

    public ExplosionControllerEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public void explode(double radius, float baseDamage, float powerMultiplier) {
        if (this.level().isClientSide) {
            return;
        }

        this.active = true;
        this.radius = Math.max(0.0, radius);
        this.baseDamage = Math.max(0.0f, baseDamage);
        this.powerMultiplier = Math.max(0.0f, powerMultiplier);

        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 center = this.position();
            LargeExplosionDamage.apply(serverLevel, this, center, this.radius, this.baseDamage, this.powerMultiplier);
        }

        this.workList = LargeExplosionPrecompute.compute(this.level(), this.blockPosition(), this.radius);
        this.nextWorkIndex = 0;

        if (workList == null || workList.size() == 0) {
            this.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide || !active) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (workList == null) {
            this.discard();
            return;
        }

        int maxPerTick = Math.max(1, ServerConfig.LARGE_EXPLOSION_MAX_BLOCKS_PER_TICK.get());
        int remaining = workList.size() - nextWorkIndex;
        int budget = Math.min(maxPerTick, remaining);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < budget; i++) {
            long packedPos = workList.positionAt(nextWorkIndex);
            nextWorkIndex++;

            pos.set(packedPos);
            if (serverLevel.isOutsideBuildHeight(pos)) {
                continue;
            }

            BlockState state = serverLevel.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (BlockImmutabilityUtil.isBlockImmutable(state)) {
                continue;
            }
            if (state.getDestroySpeed(serverLevel, pos) < 0.0f) {
                continue;
            }

            boolean removed = serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), SILENT_UPDATE_FLAGS);
            if (!removed) {
                defer(packedPos);
            }
        }

        if (nextWorkIndex >= workList.size()) {
            if (deferredSize > 0) {
                rollDeferredIntoWork();
                return;
            }
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    private void defer(long packedPos) {
        if (deferredPositions == null) {
            deferredPositions = new long[1024];
        }
        if (deferredSize >= deferredPositions.length) {
            long[] next = new long[deferredPositions.length + (deferredPositions.length >> 1)];
            System.arraycopy(deferredPositions, 0, next, 0, deferredSize);
            deferredPositions = next;
        }
        deferredPositions[deferredSize] = packedPos;
        deferredSize++;
    }

    private void rollDeferredIntoWork() {
        ExplosionWorkList list = new ExplosionWorkList(deferredSize);
        for (int i = 0; i < deferredSize; i++) {
            list.add(deferredPositions[i], 0);
        }
        this.workList = list;
        this.nextWorkIndex = 0;
        this.deferredSize = 0;
    }
}

