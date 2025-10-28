package com.github.ars_zero.common.entity;

import com.github.ars_zero.ArsZero;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockGroupEntity extends net.minecraft.world.entity.Entity {
    private static final EntityDataAccessor<CompoundTag> BLOCK_DATA = SynchedEntityData.defineId(BlockGroupEntity.class, EntityDataSerializers.COMPOUND_TAG);
    
    private final List<BlockData> blocks = new ArrayList<>();
    private final Map<BlockPos, BlockEntity> tileEntities = new HashMap<>();
    
    public BlockGroupEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
        this.setNoGravity(true);
    }
    
    public static BlockGroupEntity create(Level level, List<BlockPos> positions) {
        BlockGroupEntity entity = new BlockGroupEntity(com.github.ars_zero.registry.ModEntities.BLOCK_GROUP.get(), level);
        entity.addBlocks(positions);
        return entity;
    }
    
    public void addBlocks(List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (level().isOutsideBuildHeight(pos)) continue;
            
            BlockState state = level().getBlockState(pos);
            if (state.isAir()) continue;
            
            BlockEntity tileEntity = level().getBlockEntity(pos);
            if (tileEntity != null) {
                tileEntities.put(pos, tileEntity);
            }
            
            Vec3 relativePos = Vec3.atCenterOf(pos).subtract(this.position());
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
            tileEntities.put(pos, tileEntity);
        }
        
        Vec3 relativePos = Vec3.atCenterOf(pos).subtract(this.position());
        blocks.add(new BlockData(state, relativePos, pos));
        
        updateBoundingBox();
        syncBlockData();
    }
    
    public void removeOriginalBlocks() {
        for (BlockData blockData : blocks) {
            BlockPos originalPos = blockData.originalPosition;
            if (!level().isOutsideBuildHeight(originalPos)) {
                level().setBlock(originalPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
    
    public void placeBlocks() {
        placeBlocks(0.0f);
    }
    
    public void placeBlocks(float rotationYaw) {
        for (BlockData blockData : blocks) {
            Vec3 rotatedPos = rotateVector(blockData.relativePosition, rotationYaw);
            BlockPos newPos = BlockPos.containing(this.position().add(rotatedPos));
            
            if (level().isOutsideBuildHeight(newPos)) continue;
            
            BlockState state = blockData.blockState;
            if (level().getBlockState(newPos).canBeReplaced()) {
                level().setBlock(newPos, state, Block.UPDATE_ALL);
                
                // Note: BlockEntity data restoration is complex and may require custom handling
                // For now, we'll skip tile entity restoration to avoid compilation issues
            }
        }
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
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        }
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("blocks", Tag.TAG_LIST)) {
            ListTag blocksTag = compound.getList("blocks", Tag.TAG_COMPOUND);
            blocks.clear();
            
            for (int i = 0; i < blocksTag.size(); i++) {
                CompoundTag blockTag = blocksTag.getCompound(i);
                BlockState state = NbtUtils.readBlockState(level().holderLookup(net.minecraft.core.registries.Registries.BLOCK), blockTag.getCompound("state"));
                CompoundTag posTag = blockTag.getCompound("pos");
                Vec3 relativePos = new Vec3(posTag.getDouble("x"), posTag.getDouble("y"), posTag.getDouble("z"));
                BlockPos originalPos = NbtUtils.readBlockPos(blockTag, "original").orElse(BlockPos.ZERO);
                blocks.add(new BlockData(state, relativePos, originalPos));
            }
            
            updateBoundingBox();
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
            blocksTag.add(blockTag);
        }
        
        compound.put("blocks", blocksTag);
    }
    
    public List<BlockData> getBlocks() {
        return new ArrayList<>(blocks);
    }
    
    public boolean isEmpty() {
        return blocks.isEmpty();
    }
    
    public int getBlockCount() {
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
    
    private static class BlockData {
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