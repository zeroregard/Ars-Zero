package com.github.ars_zero.common.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.util.BlockImmutabilityUtil;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.ANFakePlayer;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlockGroupEntity extends Entity {
    private static final EntityDataAccessor<CompoundTag> BLOCK_DATA = SynchedEntityData.defineId(BlockGroupEntity.class, EntityDataSerializers.COMPOUND_TAG);
    
    private final List<BlockData> blocks = new ArrayList<>();
    private final Map<BlockPos, CompoundTag> blockEntityData = new HashMap<>();
    @Nullable
    private UUID casterUUID;
    private float originalYRot = 0.0f;
    private int lifespan = 20;
    private int maxLifeSpan = 20;
    
    public BlockGroupEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
        this.setNoGravity(true);
    }
    
    public void setCasterUUID(@Nullable UUID casterUUID) {
        this.casterUUID = casterUUID;
    }
    
    @Nullable
    public UUID getCasterUUID() {
        return casterUUID;
    }
    
    public void setOriginalYRot(float yRot) {
        this.originalYRot = yRot;
    }
    
    public float getOriginalYRot() {
        return originalYRot;
    }
    
    public static BlockGroupEntity create(Level level, List<BlockPos> positions) {
        BlockGroupEntity entity = new BlockGroupEntity(ModEntities.BLOCK_GROUP.get(), level);
        entity.addBlocks(positions);
        return entity;
    }

     public void addLifespan(int extraTicks) {
        this.lifespan += extraTicks;
        if(this.lifespan > maxLifeSpan) {
            this.lifespan = maxLifeSpan;
        }
    }
    
    public void addBlocks(List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (level().isOutsideBuildHeight(pos)) {
                continue;
            }
            
            BlockState state = level().getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            
            BlockEntity tileEntity = level().getBlockEntity(pos);
            if (tileEntity != null) {
                blockEntityData.put(pos, tileEntity.saveWithoutMetadata(level().registryAccess()));
            }
            
            Vec3 entityPos = this.position();
            Vec3 worldCenterPos = Vec3.atCenterOf(pos);
            Vec3 relativePos = worldCenterPos.subtract(entityPos);
            
            blocks.add(new BlockData(state, relativePos, pos));
        }
        
        updateBoundingBox();
        syncBlockData();
    }
    
    public void addBlocksWithStates(List<BlockPos> positions, Map<BlockPos, BlockState> capturedStates) {
        for (BlockPos pos : positions) {
            BlockState state = capturedStates.get(pos);
            if (state == null || state.isAir()) {
                continue;
            }
            
            BlockEntity tileEntity = level().getBlockEntity(pos);
            if (tileEntity != null) {
                blockEntityData.put(pos, tileEntity.saveWithoutMetadata(level().registryAccess()));
            }
            
            Vec3 entityPos = this.position();
            Vec3 worldCenterPos = Vec3.atCenterOf(pos);
            Vec3 relativePos = worldCenterPos.subtract(entityPos);
            
            blocks.add(new BlockData(state, relativePos, pos));
        }
        
        updateBoundingBox();
        syncBlockData();
    }
    
    public void addBlock(BlockPos pos) {
        if (level().isOutsideBuildHeight(pos)) return;
        
        BlockState state = level().getBlockState(pos);
        if (state.isAir()) return;
        
        BlockEntity tileEntity = level().getBlockEntity(pos);
        if (tileEntity != null) {
            blockEntityData.put(pos, tileEntity.saveWithoutMetadata(level().registryAccess()));
        }
        
        Vec3 relativePos = Vec3.atCenterOf(pos).subtract(this.position());
        blocks.add(new BlockData(state, relativePos, pos));
        
        updateBoundingBox();
        syncBlockData();
    }
    
    public void removeOriginalBlocks() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        
        LivingEntity caster = getCaster(serverLevel);
        
        for (BlockData blockData : blocks) {
            BlockPos originalPos = blockData.originalPosition;
            if (!level().isOutsideBuildHeight(originalPos)) {
                BlockState beforeState = level().getBlockState(originalPos);
                
                if (BlockImmutabilityUtil.isBlockImmutable(beforeState)) {
                    continue;
                }
                
                if (!beforeState.isAir()) {
                    if (caster != null && !BlockUtil.destroyRespectsClaim(caster, level(), originalPos)) {
                        continue;
                    }
                    
                    level().setBlock(originalPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }
    
    @Nullable
    private LivingEntity getCaster(ServerLevel serverLevel) {
        if (casterUUID == null) return null;
        return serverLevel.getPlayerByUUID(casterUUID);
    }
    
    public List<BlockPos> placeBlocks() {
        return placeBlocks(0.0f);
    }
    
    public List<BlockPos> placeBlocks(float rotationYaw) {
        List<BlockPos> placedPositions = new ArrayList<>();
        if (!(level() instanceof ServerLevel serverLevel)) return placedPositions;
        
        Player fakePlayer = ANFakePlayer.getPlayer(serverLevel, casterUUID);

        for (BlockData blockData : blocks) {
            BlockState originalState = blockData.blockState;
            Vec3 relativePos = blockData.relativePosition;
            BlockPos targetPos = BlockPos.containing(this.position().add(relativePos));
            
            BlockState stateForPlacement = originalState;
            boolean placed = false;
            
            if (!level().isOutsideBuildHeight(targetPos)) {
                BlockState existingState = level().getBlockState(targetPos);
                
                if (BlockImmutabilityUtil.isBlockImmutable(existingState)) {
                    dropBlockAsItem(originalState, blockData.originalPosition);
                    continue;
                }
                
                if (existingState.canBeReplaced()) {
                    var event = NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level().dimension(), level(), targetPos), existingState, fakePlayer));
                    if (event.isCanceled()) {
                        dropBlockAsItem(originalState, blockData.originalPosition);
                        continue;
                    }
                    
                    if (stateForPlacement.canSurvive(level(), targetPos)) {
                        if (level().setBlock(targetPos, stateForPlacement, Block.UPDATE_ALL)) {
                            placed = true;
                            placedPositions.add(targetPos);
                            
                            CompoundTag entityData = blockEntityData.get(blockData.originalPosition);
                            if (entityData != null && stateForPlacement.hasBlockEntity()) {
                                BlockEntity blockEntity = level().getBlockEntity(targetPos);
                                if (blockEntity != null) {
                                    try {
                                        blockEntity.loadWithComponents(entityData, level().registryAccess());
                                        blockEntity.setChanged();
                                    } catch (Exception exception) {
                                        ArsZero.LOGGER.warn("Failed to restore BlockEntity data at {}", targetPos, exception);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Only drop as item if placement failed
            if (!placed) {
                dropBlockAsItem(originalState, blockData.originalPosition);
            }
        }
        
        return placedPositions;
    }

    private Rotation getRotationForYaw(float yawDegrees) {
        float normalized = ((yawDegrees % 360.0f) + 360.0f) % 360.0f;

        if (normalized >= 315.0f || normalized < 45.0f) {
            return Rotation.NONE;
        } else if (normalized < 135.0f) {
            return Rotation.CLOCKWISE_90;
        } else if (normalized < 225.0f) {
            return Rotation.CLOCKWISE_180;
        } else {
            return Rotation.COUNTERCLOCKWISE_90;
        }
    }
    
    private void dropBlockAsItem(BlockState state, BlockPos originalPos) {
        if (level().isClientSide || !level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }
        
        Block block = state.getBlock();
        ItemStack itemStack = new ItemStack(block);
        
        Containers.dropItemStack(level(), 
            this.getX(), this.getY(), this.getZ(), itemStack);
    }
    
    private Vec3 rotateVector(Vec3 vector, float yawDegrees) {
        if (yawDegrees == 0.0f) return vector;
        
        double yawRad = Math.toRadians(yawDegrees);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        
        return new Vec3(
            vector.x * cos - vector.z * sin,
            vector.y,
            vector.x * sin + vector.z * cos
        );
    }
    
    private void updateBoundingBox() {
        if (blocks.isEmpty()) {
            setBoundingBox(new AABB(0, 0, 0, 1, 1, 1));
            return;
        }
        
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;
        
        for (BlockData blockData : blocks) {
            Vec3 pos = blockData.relativePosition;
            minX = Math.min(minX, pos.x - 0.5);
            minY = Math.min(minY, pos.y - 0.5);
            minZ = Math.min(minZ, pos.z - 0.5);
            maxX = Math.max(maxX, pos.x + 0.5);
            maxY = Math.max(maxY, pos.y + 0.5);
            maxZ = Math.max(maxZ, pos.z + 0.5);
        }
        
        setBoundingBox(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
    }
    
    private void syncBlockData() {
        CompoundTag tag = new CompoundTag();
        ListTag blocksTag = new ListTag();
        
        for (BlockData blockData : blocks) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.put("state", NbtUtils.writeBlockState(blockData.blockState));
            CompoundTag posTag = new CompoundTag();
            posTag.putDouble("x", blockData.relativePosition.x);
            posTag.putDouble("y", blockData.relativePosition.y);
            posTag.putDouble("z", blockData.relativePosition.z);
            blockTag.put("pos", posTag);
            blockTag.put("original", NbtUtils.writeBlockPos(blockData.originalPosition));
            
            CompoundTag entityData = blockEntityData.get(blockData.originalPosition);
            if (entityData != null) {
                blockTag.put("entityData", entityData);
            }
            
            blocksTag.add(blockTag);
        }
        
        tag.put("blocks", blocksTag);
        this.entityData.set(BLOCK_DATA, tag);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(BLOCK_DATA, new CompoundTag());
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!level().isClientSide) {
            age();
            Vec3 deltaMovement = this.getDeltaMovement();
            this.move(MoverType.SELF, deltaMovement);
            this.setDeltaMovement(deltaMovement.scale(0.98));
        }
    }

    private void age() {
        if (lifespan > 0) {
            lifespan--;
        }

        if (lifespan <= 0 && !blocks.isEmpty()) {
            placeAndDiscard();
        }
    }

    public void placeAndDiscard() {
        float rotation = 0.0f; // fallback rotation
        float nearestRotation = getNearest90DegreeRotation(rotation);
        placeBlocks(nearestRotation);
        clearBlocks();
        remove(RemovalReason.DISCARDED);
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
        
        if (source.getDirectEntity() instanceof Projectile projectile) {
            projectile.discard();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean isPickable() {
        return true;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    public void clearBlocks() {
        blocks.clear();
        blockEntityData.clear();
        syncBlockData();
    }
    
    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide && reason == RemovalReason.DISCARDED && !blocks.isEmpty()) {
            placeBlocks(0.0f);
            clearBlocks();
        }
        
        super.remove(reason);
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("blocks", Tag.TAG_LIST)) {
            ListTag blocksTag = compound.getList("blocks", Tag.TAG_COMPOUND);
            blocks.clear();
            blockEntityData.clear();
            
            for (int i = 0; i < blocksTag.size(); i++) {
                CompoundTag blockTag = blocksTag.getCompound(i);
                BlockState state = NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), blockTag.getCompound("state"));
                
                if (BlockImmutabilityUtil.isBlockImmutable(state)) {
                    ArsZero.LOGGER.warn("BlockGroupEntity loaded immutable block {} from NBT, skipping", state.getBlock());
                    continue;
                }
                
                CompoundTag posTag = blockTag.getCompound("pos");
                Vec3 relativePos = new Vec3(posTag.getDouble("x"), posTag.getDouble("y"), posTag.getDouble("z"));
                BlockPos originalPos = NbtUtils.readBlockPos(blockTag, "original").orElse(BlockPos.ZERO);
                blocks.add(new BlockData(state, relativePos, originalPos));
                
                if (blockTag.contains("entityData", Tag.TAG_COMPOUND)) {
                    blockEntityData.put(originalPos, blockTag.getCompound("entityData"));
                }
            }
            
            updateBoundingBox();
        }
        
        if (compound.contains("casterUUID")) {
            casterUUID = compound.getUUID("casterUUID");
        }
        
        if (compound.contains("originalYRot", Tag.TAG_FLOAT)) {
            originalYRot = compound.getFloat("originalYRot");
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        ListTag blocksTag = new ListTag();
        
        for (BlockData blockData : blocks) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.put("state", NbtUtils.writeBlockState(blockData.blockState));
            CompoundTag posTag = new CompoundTag();
            posTag.putDouble("x", blockData.relativePosition.x);
            posTag.putDouble("y", blockData.relativePosition.y);
            posTag.putDouble("z", blockData.relativePosition.z);
            blockTag.put("pos", posTag);
            blockTag.put("original", NbtUtils.writeBlockPos(blockData.originalPosition));
            
            CompoundTag entityData = blockEntityData.get(blockData.originalPosition);
            if (entityData != null) {
                blockTag.put("entityData", entityData);
            }
            
            blocksTag.add(blockTag);
        }
        
        compound.put("blocks", blocksTag);
        
        if (casterUUID != null) {
            compound.putUUID("casterUUID", casterUUID);
        }
        
        compound.putFloat("originalYRot", originalYRot);
    }
    
    public List<BlockData> getBlocks() {
        if (level().isClientSide) {
            return getBlocksFromSyncedData();
        }
        return new ArrayList<>(blocks);
    }
    
    private List<BlockData> getBlocksFromSyncedData() {
        List<BlockData> clientBlocks = new ArrayList<>();
        CompoundTag tag = this.entityData.get(BLOCK_DATA);
        
        if (tag != null && tag.contains("blocks", Tag.TAG_LIST)) {
            ListTag blocksTag = tag.getList("blocks", Tag.TAG_COMPOUND);
            
            for (int i = 0; i < blocksTag.size(); i++) {
                CompoundTag blockTag = blocksTag.getCompound(i);
                try {
                    BlockState state = NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), blockTag.getCompound("state"));
                    
                    if (BlockImmutabilityUtil.isBlockImmutable(state)) {
                        ArsZero.LOGGER.warn("BlockGroupEntity synced immutable block {} to client, skipping", state.getBlock());
                        continue;
                    }
                    
                    CompoundTag posTag = blockTag.getCompound("pos");
                    Vec3 relativePos = new Vec3(posTag.getDouble("x"), posTag.getDouble("y"), posTag.getDouble("z"));
                    BlockPos originalPos = NbtUtils.readBlockPos(blockTag, "original").orElse(BlockPos.ZERO);
                    clientBlocks.add(new BlockData(state, relativePos, originalPos));
                } catch (Exception e) {
                    ArsZero.LOGGER.warn("Failed to read block data from synced data", e);
                }
            }
        }
        
        return clientBlocks;
    }
    
    public boolean isEmpty() {
        if (level().isClientSide) {
            return getBlocksFromSyncedData().isEmpty();
        }
        return blocks.isEmpty();
    }
    
    public int getBlockCount() {
        if (level().isClientSide) {
            return getBlocksFromSyncedData().size();
        }
        return blocks.size();
    }
    
    public float getNearest90DegreeRotation(float currentYaw) {
        float normalizedYaw = currentYaw % 360.0f;
        if (normalizedYaw < 0) normalizedYaw += 360.0f;
        
        if (normalizedYaw < 45.0f || normalizedYaw >= 315.0f) {
            return 0.0f;
        } else if (normalizedYaw < 135.0f) {
            return 90.0f;
        } else if (normalizedYaw < 225.0f) {
            return 180.0f;
        } else {
            return 270.0f;
        }
    }
    
    public static class BlockData {
        public final BlockState blockState;
        public final Vec3 relativePosition;
        public final BlockPos originalPosition;
        
        public BlockData(BlockState blockState, Vec3 relativePosition, BlockPos originalPosition) {
            this.blockState = blockState;
            this.relativePosition = relativePosition;
            this.originalPosition = originalPosition;
        }
    }
}