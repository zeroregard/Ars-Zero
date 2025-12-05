package com.github.ars_zero.common.block;

import com.github.ars_zero.common.entity.ArcaneVoxelEntity;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.IceVoxelEntity;
import com.github.ars_zero.common.entity.LightningVoxelEntity;
import com.github.ars_zero.common.entity.PoisonVoxelEntity;
import com.github.ars_zero.common.entity.StoneVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.common.entity.WindVoxelEntity;
import com.github.ars_zero.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public class VoxelSpawnerBlockEntity extends BlockEntity {
    
    private static final int VOXEL_LIFETIME_TICKS = 100;
    private static final int WAIT_AFTER_DESTROY_TICKS = 20;
    
    private BaseVoxelEntity currentVoxel;
    private UUID currentVoxelUUID;
    private int ticksSinceSpawn = 0;
    private int waitTicks = 0;
    private boolean waiting = false;
    private boolean needsVoxelRestore = false;
    private boolean needsVoxelSearch = false;
    
    public VoxelSpawnerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.VOXEL_SPAWNER.get(), pos, blockState);
    }
    
    public void tick() {
        if (level == null || level.isClientSide) return;
        
        if (needsVoxelSearch) {
            searchForExistingVoxel();
            needsVoxelSearch = false;
        }
        
        if (needsVoxelRestore) {
            tryRestoreVoxel();
            needsVoxelRestore = false;
        }
        
        if (waiting) {
            waitTicks++;
            if (waitTicks >= WAIT_AFTER_DESTROY_TICKS) {
                waiting = false;
                waitTicks = 0;
            }
            return;
        }
        
        boolean isNull = currentVoxel == null;
        boolean isAlive = currentVoxel != null && currentVoxel.isAlive();
        boolean isOwned = currentVoxel != null && currentVoxel.isSpawnerOwned();
        
        if (isNull || !isAlive || !isOwned) {
            if (currentVoxel != null) {
                currentVoxel = null;
                currentVoxelUUID = null;
                startWaiting();
            } else {
                spawnVoxel();
            }
            return;
        }
        
        ticksSinceSpawn++;
        if (ticksSinceSpawn >= VOXEL_LIFETIME_TICKS) {
            destroyVoxel();
            startWaiting();
        }
    }
    
    private void spawnVoxel() {
        if (level == null || !(level instanceof ServerLevel)) return;
        
        BlockPos spawnPos = worldPosition.above();
        double x = spawnPos.getX() + 0.5;
        double y = spawnPos.getY() + 0.5;
        double z = spawnPos.getZ() + 0.5;
        
        VoxelSpawnerBlock.VoxelType voxelType = getVoxelType();
        
        BaseVoxelEntity voxel = switch (voxelType) {
            case FIRE -> new FireVoxelEntity(level, x, y, z, Integer.MAX_VALUE);
            case WATER -> new WaterVoxelEntity(level, x, y, z, Integer.MAX_VALUE);
            case WIND -> new WindVoxelEntity(level, x, y, z, Integer.MAX_VALUE);
            case STONE -> new StoneVoxelEntity(level, x, y, z, Integer.MAX_VALUE);
            case ICE -> new IceVoxelEntity(level, x, y, z, Integer.MAX_VALUE);
            case LIGHTNING -> new LightningVoxelEntity(level, x, y, z, Integer.MAX_VALUE);
            case POISON -> new PoisonVoxelEntity(level, x, y, z, Integer.MAX_VALUE);
            default -> new ArcaneVoxelEntity(level, x, y, z, Integer.MAX_VALUE);
        };
        
        voxel.setSize(1.0f);
        voxel.setDeltaMovement(0, 0, 0);
        voxel.setPickable(false);
        voxel.setSpawnerOwned(true);
        if (!(voxel instanceof StoneVoxelEntity) && !(voxel instanceof IceVoxelEntity) && !(voxel instanceof LightningVoxelEntity)) {
            voxel.setNoGravityCustom(true);
        }
        
        level.addFreshEntity(voxel);
        
        currentVoxel = voxel;
        currentVoxelUUID = voxel.getUUID();
        ticksSinceSpawn = 0;
    }
    
    private void destroyVoxel() {
        if (currentVoxel != null && currentVoxel.isAlive()) {
            currentVoxel.discard();
        }
        currentVoxel = null;
        currentVoxelUUID = null;
        ticksSinceSpawn = 0;
    }
    
    private void searchForExistingVoxel() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        BlockPos searchCenter = worldPosition.above();
        AABB searchBox = new AABB(searchCenter).inflate(5.0);
        List<BaseVoxelEntity> nearbyVoxels = serverLevel.getEntitiesOfClass(BaseVoxelEntity.class, searchBox);
        
        for (BaseVoxelEntity voxel : nearbyVoxels) {
            if (voxel.isSpawnerOwned() && voxel.isAlive()) {
                VoxelSpawnerBlock.VoxelType expectedType = getVoxelType();
                boolean typeMatches = switch (expectedType) {
                    case FIRE -> voxel instanceof FireVoxelEntity;
                    case WATER -> voxel instanceof WaterVoxelEntity;
                    case WIND -> voxel instanceof WindVoxelEntity;
                    case STONE -> voxel instanceof StoneVoxelEntity;
                    case ICE -> voxel instanceof IceVoxelEntity;
                    case LIGHTNING -> voxel instanceof LightningVoxelEntity;
                    case ARCANE -> voxel instanceof ArcaneVoxelEntity;
                    case POISON -> voxel instanceof PoisonVoxelEntity;
                };
                
                if (typeMatches) {
                    currentVoxel = voxel;
                    currentVoxelUUID = voxel.getUUID();
                    return;
                }
            }
        }
    }
    
    private void tryRestoreVoxel() {
        if (currentVoxelUUID == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        Entity entity = serverLevel.getEntity(currentVoxelUUID);
        if (entity instanceof BaseVoxelEntity voxel && voxel.isAlive()) {
            currentVoxel = voxel;
        } else {
            currentVoxel = null;
            currentVoxelUUID = null;
            startWaiting();
        }
    }
    
    private void startWaiting() {
        waiting = true;
        waitTicks = 0;
    }
    
    private VoxelSpawnerBlock.VoxelType getVoxelType() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof VoxelSpawnerBlock spawnerBlock) {
            return spawnerBlock.getVoxelType();
        }
        return VoxelSpawnerBlock.VoxelType.ARCANE;
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ticksSinceSpawn", ticksSinceSpawn);
        tag.putInt("waitTicks", waitTicks);
        tag.putBoolean("waiting", waiting);
        if (currentVoxelUUID != null) {
            tag.putUUID("currentVoxelUUID", currentVoxelUUID);
        }
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ticksSinceSpawn = tag.getInt("ticksSinceSpawn");
        waitTicks = tag.getInt("waitTicks");
        waiting = tag.getBoolean("waiting");
        if (tag.hasUUID("currentVoxelUUID")) {
            currentVoxelUUID = tag.getUUID("currentVoxelUUID");
            needsVoxelRestore = true;
        }
        needsVoxelSearch = true;
    }
}

