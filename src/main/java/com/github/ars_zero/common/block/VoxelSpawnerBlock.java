package com.github.ars_zero.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class VoxelSpawnerBlock extends Block implements EntityBlock {
    
    public enum VoxelType {
        ARCANE,
        FIRE,
        WATER
    }
    
    private final VoxelType voxelType;
    
    public VoxelSpawnerBlock(Properties properties, VoxelType voxelType) {
        super(properties);
        this.voxelType = voxelType;
    }
    
    public VoxelType getVoxelType() {
        return voxelType;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VoxelSpawnerBlockEntity(pos, state);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof VoxelSpawnerBlockEntity spawner) {
                spawner.tick();
            }
        };
    }
}

