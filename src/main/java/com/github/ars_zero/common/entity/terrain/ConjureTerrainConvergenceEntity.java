package com.github.ars_zero.common.entity.terrain;

import com.github.ars_zero.common.entity.AbstractConvergenceEntity;
import com.github.ars_zero.common.config.ServerConfig;
import com.github.ars_zero.common.structure.ConvergenceStructureHelper;
import com.github.ars_zero.common.util.BlockProtectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConjureTerrainConvergenceEntity extends AbstractConvergenceEntity {
    private static final EntityDataAccessor<Integer> DATA_SIZE = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BUILDING = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int DEFAULT_SIZE = 3;
    private static final int MIN_SIZE = 1;
    private static final int BLOCKS_PER_TICK = 1;

    @Nullable
    private UUID casterUuid = null;

    private boolean building = false;
    private final List<BlockPos> buildQueue = new ArrayList<>();
    private int buildIndex = 0;

    public ConjureTerrainConvergenceEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    public void setCasterUUID(@Nullable UUID casterUuid) {
        this.casterUuid = casterUuid;
    }

    public int getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public boolean isBuilding() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_BUILDING);
        }
        return this.building;
    }

    public void setSize(int size) {
        int clampedOdd = clampSize(size);
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_SIZE, clampedOdd);
        }
    }

    public void adjustSizeStep(int direction) {
        if (direction == 0) {
            return;
        }
        if (isBuilding() || getLifespan() <= 0) {
            return;
        }
        int current = getSize();
        int next = current + (direction > 0 ? 1 : -1);
        setSize(next);
    }

    public int getMinOffset() {
        return ConvergenceStructureHelper.minOffset(getSize());
    }

    public int getMaxOffset() {
        return ConvergenceStructureHelper.maxOffset(getSize());
    }

    @Override
    public boolean shouldStart() {
        return !isBuilding() && super.shouldStart();
    }

    @Override
    protected void onLifespanReached() {
        if (this.level().isClientSide) {
            return;
        }
        if (this.building) {
            return;
        }
        startBuilding();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.building) {
            tickBuild();
        }
    }

    @Override
    public boolean isInvisible() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SIZE, DEFAULT_SIZE);
        builder.define(DATA_BUILDING, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("size")) {
            int size = compound.getInt("size");
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_SIZE, clampSize(size));
            }
        }
        if (compound.contains("building")) {
            this.building = compound.getBoolean("building");
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_BUILDING, this.building);
            }
        }
        if (compound.contains("build_index")) {
            this.buildIndex = Math.max(0, compound.getInt("build_index"));
        }
        if (compound.contains("caster_uuid")) {
            this.casterUuid = compound.getUUID("caster_uuid");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("size", getSize());
        compound.putBoolean("building", this.building);
        compound.putInt("build_index", this.buildIndex);
        if (this.casterUuid != null) {
            compound.putUUID("caster_uuid", this.casterUuid);
        }
    }

    private void startBuilding() {
        this.building = true;
        this.entityData.set(DATA_BUILDING, true);
        this.buildQueue.clear();
        this.buildIndex = 0;

        BlockPos center = BlockPos.containing(this.position());
        this.buildQueue.addAll(ConvergenceStructureHelper.generate(center, getSize(), ConvergenceStructureHelper.Shape.CUBE));
    }

    private void tickBuild() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.buildIndex >= this.buildQueue.size()) {
            this.discard();
            return;
        }

        Player claimActor = getClaimActor(serverLevel);
        BlockState terrainState = Blocks.STONE.defaultBlockState();

        this.buildIndex = ConvergenceStructureHelper.placeNext(serverLevel, this.buildQueue, this.buildIndex, BLOCKS_PER_TICK,
                terrainState, claimActor);
    }

    @Nullable
    private Player getClaimActor(ServerLevel level) {
        if (this.casterUuid == null) {
            return null;
        }
        if (level.getServer() == null || level.getServer().getPlayerList() == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(this.casterUuid);
    }

    private static int clampSize(int size) {
        int maxSize = getMaxSize();
        return Math.max(MIN_SIZE, Math.min(maxSize, size));
    }

    private static int getMaxSize() {
        if (ServerConfig.CONJURE_TERRAIN_MAX_SIZE == null) {
            return 64;
        }
        return Math.max(MIN_SIZE, ServerConfig.CONJURE_TERRAIN_MAX_SIZE.get());
    }
}

