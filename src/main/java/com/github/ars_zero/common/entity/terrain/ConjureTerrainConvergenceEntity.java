package com.github.ars_zero.common.entity.terrain;

import com.github.ars_zero.common.entity.AbstractConvergenceEntity;
import com.github.ars_zero.common.config.ServerConfig;
import com.github.ars_zero.common.structure.ConvergenceStructureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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
import java.util.Optional;
import java.util.UUID;

public class ConjureTerrainConvergenceEntity extends AbstractConvergenceEntity {
    private static final EntityDataAccessor<Integer> DATA_SIZE = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BUILDING = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PAUSED = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_CASTER_UUID = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_HAS_MARKER_POS = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> DATA_MARKER_POS = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.BLOCK_POS);

    private static final int DEFAULT_SIZE = 3;
    private static final int MIN_SIZE = 1;
    private static final int BLOCKS_PER_TICK = 1;

    @Nullable
    private UUID casterUuid = null;
    @Nullable
    private BlockPos markerPos = null;

    private boolean building = false;
    private boolean paused = false;
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
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_CASTER_UUID, Optional.ofNullable(casterUuid));
        }
    }

    @Nullable
    public UUID getCasterUUID() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_CASTER_UUID).orElse(null);
        }
        return this.casterUuid;
    }

    public void setMarkerPos(@Nullable BlockPos markerPos) {
        this.markerPos = markerPos;
        if (!this.level().isClientSide) {
            boolean has = markerPos != null;
            this.entityData.set(DATA_HAS_MARKER_POS, has);
            if (has) {
                this.entityData.set(DATA_MARKER_POS, markerPos);
            }
        }
    }

    @Nullable
    public BlockPos getMarkerPos() {
        if (this.level().isClientSide) {
            if (!this.entityData.get(DATA_HAS_MARKER_POS)) {
                return null;
            }
            return this.entityData.get(DATA_MARKER_POS);
        }
        return this.markerPos;
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

    public boolean isPaused() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_PAUSED);
        }
        return this.paused;
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
        if (this.building && !this.paused) {
            tickBuild();
        }
    }

    @Override
    public boolean isInvisible() {
        return false;
    }

    @Override
    public boolean isInvisibleTo(Player player) {
        UUID caster = getCasterUUID();
        if (caster == null || player == null) {
            return true;
        }
        return !caster.equals(player.getUUID());
    }

    @Override
    public boolean isPickable() {
        return isBuilding();
    }

    @Override
    public float getPickRadius() {
        return isBuilding() ? 0.75f : 0.0f;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!isBuilding()) {
            return InteractionResult.PASS;
        }
        UUID caster = getCasterUUID();
        if (caster == null || !caster.equals(player.getUUID())) {
            return InteractionResult.PASS;
        }
        this.paused = !this.paused;
        this.entityData.set(DATA_PAUSED, this.paused);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) {
            return true;
        }
        if (!isBuilding()) {
            return false;
        }
        if (!(source.getEntity() instanceof Player player)) {
            return false;
        }
        UUID caster = getCasterUUID();
        if (caster == null || !caster.equals(player.getUUID())) {
            return false;
        }
        this.discard();
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SIZE, DEFAULT_SIZE);
        builder.define(DATA_BUILDING, false);
        builder.define(DATA_PAUSED, false);
        builder.define(DATA_CASTER_UUID, Optional.empty());
        builder.define(DATA_HAS_MARKER_POS, false);
        builder.define(DATA_MARKER_POS, BlockPos.ZERO);
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
        if (compound.contains("paused")) {
            this.paused = compound.getBoolean("paused");
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_PAUSED, this.paused);
            }
        }
        if (compound.contains("build_index")) {
            this.buildIndex = Math.max(0, compound.getInt("build_index"));
        }
        if (compound.contains("caster_uuid")) {
            this.casterUuid = compound.getUUID("caster_uuid");
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_CASTER_UUID, Optional.ofNullable(this.casterUuid));
            }
        }
        if (compound.contains("marker_x") && compound.contains("marker_y") && compound.contains("marker_z")) {
            int x = compound.getInt("marker_x");
            int y = compound.getInt("marker_y");
            int z = compound.getInt("marker_z");
            this.markerPos = new BlockPos(x, y, z);
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_HAS_MARKER_POS, true);
                this.entityData.set(DATA_MARKER_POS, this.markerPos);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("size", getSize());
        compound.putBoolean("building", this.building);
        compound.putBoolean("paused", this.paused);
        compound.putInt("build_index", this.buildIndex);
        if (this.casterUuid != null) {
            compound.putUUID("caster_uuid", this.casterUuid);
        }
        BlockPos marker = getMarkerPos();
        if (marker != null) {
            compound.putInt("marker_x", marker.getX());
            compound.putInt("marker_y", marker.getY());
            compound.putInt("marker_z", marker.getZ());
        }
    }

    private void startBuilding() {
        this.building = true;
        this.paused = false;
        this.entityData.set(DATA_BUILDING, true);
        this.entityData.set(DATA_PAUSED, false);
        this.buildQueue.clear();
        this.buildIndex = 0;

        BlockPos center = BlockPos.containing(this.position());
        this.buildQueue.addAll(ConvergenceStructureHelper.generate(center, getSize(), ConvergenceStructureHelper.Shape.CUBE));

        BlockPos marker = getMarkerPos();
        if (marker != null) {
            this.setPos(marker.getX() + 0.5, marker.getY(), marker.getZ() + 0.5);
        }
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
        UUID caster = getCasterUUID();
        if (caster == null) {
            return null;
        }
        if (level.getServer() == null || level.getServer().getPlayerList() == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(caster);
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

