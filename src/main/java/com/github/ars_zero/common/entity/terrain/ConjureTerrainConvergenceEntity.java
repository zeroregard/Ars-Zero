package com.github.ars_zero.common.entity.terrain;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.common.entity.AbstractConvergenceEntity;
import com.github.ars_zero.common.entity.IAltScrollable;
import com.github.ars_zero.common.glyph.convergence.EffectConvergence;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import com.github.ars_zero.common.structure.ConvergenceStructureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ConjureTerrainConvergenceEntity extends AbstractConvergenceEntity implements IAltScrollable {
    private static final EntityDataAccessor<Integer> DATA_SIZE = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BUILDING = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PAUSED = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_WAITING_FOR_MANA = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_CASTER_UUID = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_HAS_MARKER_POS = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> DATA_MARKER_POS = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> DATA_TARGET_BLOCK = SynchedEntityData
            .defineId(ConjureTerrainConvergenceEntity.class, EntityDataSerializers.BLOCK_POS);

    private static final int DEFAULT_SIZE = 5;
    private static final int MIN_SIZE = 1;
    private static final float BASE_BLOCKS_PER_TICK = 0.5f;
    private static final double BASE_MANA_COST_PER_BLOCK = 0.1;
    private static final double FREE_MANA_AMOUNT = 32.0;

    @Nullable
    private UUID casterUuid = null;
    private float earthPower = 0.0f;
    private float blockPlacementAccumulator = 0.0f;
    private double consumedMana = 0.0;
    @Nullable
    private BlockPos markerPos = null;
    @Nullable
    private BlockState terrainBlockState = null;
    private int augmentCount = 0;

    private boolean building = false;
    private boolean paused = false;
    private boolean waitingForMana = false;
    private final List<BlockPos> buildQueue = new ArrayList<>();
    private int buildIndex = 0;
    private float clientSmoothedYaw = 0f;

    public ConjureTerrainConvergenceEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 1, state -> {
            if (!isBuilding()) {
                return PlayState.STOP;
            }

            state.getController().setAnimation(RawAnimation.begin().thenPlay("tending_master"));

            if (isPaused()) {
                state.getController().setAnimationSpeed(0.0);
            } else if (isWaitingForMana()) {
                state.getController().setAnimationSpeed(0.1);
            } else {
                state.getController().setAnimationSpeed(1.0);
            }

            return PlayState.CONTINUE;
        }));
    }

    public void setCasterUUID(@Nullable UUID casterUuid) {
        this.casterUuid = casterUuid;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_CASTER_UUID, Optional.ofNullable(casterUuid));
        }
    }

    public void setCaster(@Nullable LivingEntity caster) {
        if (caster == null) {
            return;
        }
        this.casterUuid = caster.getUUID();
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_CASTER_UUID, Optional.of(caster.getUUID()));
        }
        this.earthPower = getEarthPower(caster);
    }

    private float getEarthPower(LivingEntity caster) {
        if (caster instanceof Player player) {
            AttributeInstance instance = player.getAttribute(ModRegistry.EARTH_POWER);
            if (instance != null) {
                return (float) instance.getValue();
            }
        }
        return 0.0f;
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

    public boolean isWaitingForMana() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_WAITING_FOR_MANA);
        }
        return this.waitingForMana;
    }

    private void setWaitingForMana(boolean waiting) {
        this.waitingForMana = waiting;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_WAITING_FOR_MANA, waiting);
        }
    }

    public void setSize(int size) {
        int clampedOdd = clampSize(size);
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_SIZE, clampedOdd);
        }
    }

    @Override
    public void handleAltScroll(double scrollDelta) {
        if (getLifespan() <= 0 || isBuilding()) {
            return;
        }
        int direction = scrollDelta > 0 ? 1 : (scrollDelta < 0 ? -1 : 0);
        if (direction == 0) {
            return;
        }
        int current = getSize();
        int next = current + (direction > 0 ? 1 : -1);
        setSize(next);
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

    @Nullable
    public BlockPos getTargetBlock() {
        BlockPos target = this.entityData.get(DATA_TARGET_BLOCK);
        if (target.equals(BlockPos.ZERO)) {
            return null;
        }
        return target;
    }

    private void updateTargetBlock() {
        if (!this.level().isClientSide && this.building && this.buildIndex < this.buildQueue.size()) {
            BlockPos next = this.buildQueue.get(this.buildIndex);
            this.entityData.set(DATA_TARGET_BLOCK, next);
        }
    }

    public float getSmoothedYaw(float targetYaw) {
        float diff = targetYaw - this.clientSmoothedYaw;
        while (diff > 180f)
            diff -= 360f;
        while (diff < -180f)
            diff += 360f;
        this.clientSmoothedYaw += diff * 0.5f;
        while (this.clientSmoothedYaw > 180f)
            this.clientSmoothedYaw -= 360f;
        while (this.clientSmoothedYaw < -180f)
            this.clientSmoothedYaw += 360f;
        return this.clientSmoothedYaw;
    }

    public void setTerrainBlockState(BlockState blockState) {
        this.terrainBlockState = blockState;
    }

    public BlockState getTerrainBlockState() {
        if (this.terrainBlockState == null) {
            return Blocks.DIRT.defaultBlockState();
        }
        return this.terrainBlockState;
    }

    public void setAugmentCount(int count) {
        this.augmentCount = count;
    }

    public int getAugmentCount() {
        return this.augmentCount;
    }

    private int getBlockTypeFactor() {
        BlockState block = getTerrainBlockState();
        if (block.getBlock() == Blocks.DIRT || block.getBlock() == Blocks.COARSE_DIRT
                || block.getBlock() == Blocks.PODZOL || block.getBlock() == Blocks.GRASS_BLOCK
                || block.getBlock() == Blocks.GRAVEL || block.getBlock() == Blocks.MUD) {
            return 1;
        } else if (block.getBlock() == Blocks.COBBLESTONE || block.getBlock() == Blocks.COBBLED_DEEPSLATE
                || block.getBlock() == Blocks.SAND || block.getBlock() == Blocks.RED_SAND
                || block.getBlock() == Blocks.SANDSTONE || block.getBlock() == Blocks.RED_SANDSTONE) {
            return 2;
        } else {
            return 3;
        }
    }

    private double getManaCostPerBlock() {
        int blockFactor = getBlockTypeFactor();
        int augmentFactor = this.augmentCount + 1;
        return BASE_MANA_COST_PER_BLOCK * blockFactor * augmentFactor;
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
        Vec3 pos = this.position();
        this.setBoundingBox(new AABB(pos.x - 0.25, pos.y + 0.5, pos.z - 0.25, pos.x + 0.25, pos.y + 1.0, pos.z + 0.25));
        if (!this.level().isClientSide) {
            if (this.building && !this.paused) {
                tickBuild();
            }
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
        if (!this.paused) {
            setWaitingForMana(false);
        }
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
        builder.define(DATA_WAITING_FOR_MANA, false);
        builder.define(DATA_CASTER_UUID, Optional.empty());
        builder.define(DATA_HAS_MARKER_POS, false);
        builder.define(DATA_MARKER_POS, BlockPos.ZERO);
        builder.define(DATA_TARGET_BLOCK, BlockPos.ZERO);
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
        if (compound.contains("waiting_for_mana")) {
            this.waitingForMana = compound.getBoolean("waiting_for_mana");
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_WAITING_FOR_MANA, this.waitingForMana);
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
        if (compound.contains("terrain_block")) {
            ResourceLocation blockId = ResourceLocation.parse(compound.getString("terrain_block"));
            Block block = BuiltInRegistries.BLOCK.get(blockId);
            if (block != null) {
                this.terrainBlockState = block.defaultBlockState();
            }
        }
        if (compound.contains("earth_power")) {
            this.earthPower = compound.getFloat("earth_power");
        }
        if (compound.contains("block_accumulator")) {
            this.blockPlacementAccumulator = compound.getFloat("block_accumulator");
        }
        if (compound.contains("consumed_mana")) {
            this.consumedMana = compound.getDouble("consumed_mana");
        }
        if (compound.contains("augment_count")) {
            this.augmentCount = compound.getInt("augment_count");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("size", getSize());
        compound.putBoolean("building", this.building);
        compound.putBoolean("paused", this.paused);
        compound.putBoolean("waiting_for_mana", this.waitingForMana);
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
        if (this.terrainBlockState != null) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(this.terrainBlockState.getBlock());
            if (blockId != null) {
                compound.putString("terrain_block", blockId.toString());
            }
        }
        compound.putFloat("earth_power", this.earthPower);
        compound.putFloat("block_accumulator", this.blockPlacementAccumulator);
        compound.putDouble("consumed_mana", this.consumedMana);
        compound.putInt("augment_count", this.augmentCount);
    }

    private void startBuilding() {
        this.building = true;
        this.paused = false;
        this.waitingForMana = false;
        this.entityData.set(DATA_BUILDING, true);
        this.entityData.set(DATA_PAUSED, false);
        this.entityData.set(DATA_WAITING_FOR_MANA, false);
        this.buildQueue.clear();
        this.buildIndex = 0;

        BlockPos center = BlockPos.containing(this.position());
        this.buildQueue
                .addAll(ConvergenceStructureHelper.generate(center, getSize(), ConvergenceStructureHelper.Shape.CUBE));

        BlockPos marker = getMarkerPos();
        if (marker != null) {
            this.setPos(marker.getX() + 0.5, marker.getY(), marker.getZ() + 0.5);
        }
    }

    private void tickBuild() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        updateTargetBlock();
        if (this.buildIndex >= this.buildQueue.size()) {
            this.discard();
            return;
        }

        if (this.paused) {
            setWaitingForMana(false);
            return;
        }

        float rate = BASE_BLOCKS_PER_TICK * (1.0f + earthPower / 2.0f);
        this.blockPlacementAccumulator += rate;

        int blocksToPlace = (int) this.blockPlacementAccumulator;
        if (blocksToPlace <= 0) {
            setWaitingForMana(false);
            return;
        }

        Player claimActor = getClaimActor(serverLevel);
        if (claimActor == null) {
            setWaitingForMana(false);
            return;
        }

        // Check if we have enough mana for all blocks we want to place before
        // attempting
        boolean hasMana = canAffordMana(serverLevel, claimActor, blocksToPlace);
        if (!hasMana) {
            setWaitingForMana(true);
            return;
        }

        setWaitingForMana(false);

        BlockState terrainState = getTerrainBlockState();

        int oldIndex = this.buildIndex;
        this.buildIndex = ConvergenceStructureHelper.placeNext(serverLevel, this.buildQueue, this.buildIndex,
                blocksToPlace,
                terrainState, claimActor);

        // Subtract from accumulator (tracks placement attempts, not successful
        // placements)
        this.blockPlacementAccumulator -= blocksToPlace;

        int blocksPlaced = this.buildIndex - oldIndex;

        // Consume mana for each block that was actually placed
        // We've already checked that we can afford blocksToPlace, so this should
        // succeed
        // However, if somehow we run out of mana mid-way (rare edge case), we'll wait
        // next tick
        for (int i = 0; i < blocksPlaced; i++) {
            boolean consumed = consumeManaForBlock(serverLevel, claimActor);
            if (!consumed) {
                // This should never happen since we checked with canAffordMana, but handle it
                // anyway
                setWaitingForMana(true);
                return;
            }
        }
    }

    private boolean canAffordMana(ServerLevel serverLevel, Player player, int blockCount) {
        double costPerBlock = getManaCostPerBlock();
        double totalCost = blockCount * costPerBlock;
        double remainingFree = Math.max(0, FREE_MANA_AMOUNT - this.consumedMana);
        double costAfterFree = Math.max(0, totalCost - remainingFree);

        if (costAfterFree <= 0) {
            return true;
        }

        IManaCap manaCap = CapabilityRegistry.getMana(player);
        if (manaCap == null) {
            return false;
        }

        return manaCap.getCurrentMana() >= costAfterFree;
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
        return Math.max(MIN_SIZE, EffectConvergence.INSTANCE.getTerrainMaxSize());
    }

    private boolean consumeManaForBlock(ServerLevel serverLevel, Player player) {
        double costPerBlock = getManaCostPerBlock();

        if (this.consumedMana < FREE_MANA_AMOUNT) {
            double remainingFree = FREE_MANA_AMOUNT - this.consumedMana;
            if (costPerBlock <= remainingFree) {
                this.consumedMana += costPerBlock;
                return true;
            } else {
                double freeAmount = remainingFree;
                this.consumedMana += freeAmount;
                double remainingCost = costPerBlock - freeAmount;

                IManaCap manaCap = CapabilityRegistry.getMana(player);
                if (manaCap != null && manaCap.getCurrentMana() >= remainingCost) {
                    manaCap.removeMana(remainingCost);
                    this.consumedMana += remainingCost;
                    return true;
                }
                this.consumedMana -= freeAmount;
                return false;
            }
        }

        IManaCap manaCap = CapabilityRegistry.getMana(player);
        if (manaCap != null && manaCap.getCurrentMana() >= costPerBlock) {
            manaCap.removeMana(costPerBlock);
            this.consumedMana += costPerBlock;
            return true;
        }

        return false;
    }
}
