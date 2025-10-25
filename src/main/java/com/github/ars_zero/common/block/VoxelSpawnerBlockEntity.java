package com.github.ars_zero.common.block;

import com.github.ars_zero.common.entity.ArcaneVoxelEntity;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
    
    public VoxelSpawnerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.VOXEL_SPAWNER.get(), pos, blockState);
    }
    
    public void tick() {
        if (level == null || level.isClientSide) return;
        
        if (needsVoxelRestore) {
            tryRestoreVoxel();
            needsVoxelRestore = false;
        }
        
        if (waiting) {
            waitTicks++;
            if (waitTicks >= WAIT_AFTER_DESTROY_TICKS) {
                waiting = false;
                waitTicks = 0;
                com.github.ars_zero.ArsZero.LOGGER.info("Spawner finished waiting, will spawn next tick");
            }
            return;
        }
        
        boolean isNull = currentVoxel == null;
        boolean isAlive = currentVoxel != null && currentVoxel.isAlive();
        boolean isOwned = currentVoxel != null && currentVoxel.isSpawnerOwned();
        
        if (currentVoxel != null && level.getGameTime() % 20 == 0) {
            com.github.ars_zero.ArsZero.LOGGER.info("Spawner check: voxelNull={}, isAlive={}, isOwned={}", 
                isNull, isAlive, isOwned);
        }
        
        if (isNull || !isAlive || !isOwned) {
            if (currentVoxel != null) {
                com.github.ars_zero.ArsZero.LOGGER.info("Spawner detected voxel lost (alive={}, owned={}), clearing reference and starting wait", 
                    isAlive, isOwned);
                currentVoxel = null;
                currentVoxelUUID = null;
                startWaiting();
            } else {
                com.github.ars_zero.ArsZero.LOGGER.info("Spawner has no voxel, spawning new one");
                spawnVoxel();
            }
            return;
        }
        
        ticksSinceSpawn++;
        if (ticksSinceSpawn >= VOXEL_LIFETIME_TICKS) {
            com.github.ars_zero.ArsZero.LOGGER.info("Spawner voxel lifetime expired, destroying");
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
            default -> new ArcaneVoxelEntity(level, x, y, z, Integer.MAX_VALUE);
        };
        
        voxel.setSize(1.0f);
        voxel.setDeltaMovement(0, 0, 0);
        voxel.setNoGravity(true);
        voxel.setPickable(false);
        voxel.setSpawnerOwned(true);
        
        com.github.ars_zero.ArsZero.LOGGER.info("About to spawn voxel with noGravity={}, pickable={}, spawnerOwned={}", 
            voxel.isNoGravity(), voxel.isPickable(), voxel.isSpawnerOwned());
        
        level.addFreshEntity(voxel);
        
        com.github.ars_zero.ArsZero.LOGGER.info("After spawning: noGravity={}, pickable={}, spawnerOwned={}", 
            voxel.isNoGravity(), voxel.isPickable(), voxel.isSpawnerOwned());
        
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
    
    private void tryRestoreVoxel() {
        if (currentVoxelUUID == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        Entity entity = serverLevel.getEntity(currentVoxelUUID);
        if (entity instanceof BaseVoxelEntity voxel && voxel.isAlive()) {
            currentVoxel = voxel;
            com.github.ars_zero.ArsZero.LOGGER.info("Restored voxel reference after world load: size={}, owned={}", 
                voxel.getSize(), voxel.isSpawnerOwned());
        } else {
            com.github.ars_zero.ArsZero.LOGGER.info("Voxel UUID found but entity doesn't exist, starting wait cycle");
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
    }
}

