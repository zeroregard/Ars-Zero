package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.block.VoxelSpawnerBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, ArsZero.MOD_ID);
    
    public static final DeferredHolder<Block, VoxelSpawnerBlock> ARCANE_VOXEL_SPAWNER = BLOCKS.register(
        "arcane_voxel_spawner",
        () -> new VoxelSpawnerBlock(BlockBehaviour.Properties.of()
            .strength(2.0f)
            .noOcclusion(), VoxelSpawnerBlock.VoxelType.ARCANE)
    );
    
    public static final DeferredHolder<Block, VoxelSpawnerBlock> FIRE_VOXEL_SPAWNER = BLOCKS.register(
        "fire_voxel_spawner",
        () -> new VoxelSpawnerBlock(BlockBehaviour.Properties.of()
            .strength(2.0f)
            .noOcclusion(), VoxelSpawnerBlock.VoxelType.FIRE)
    );
    
    public static final DeferredHolder<Block, VoxelSpawnerBlock> WATER_VOXEL_SPAWNER = BLOCKS.register(
        "water_voxel_spawner",
        () -> new VoxelSpawnerBlock(BlockBehaviour.Properties.of()
            .strength(2.0f)
            .noOcclusion(), VoxelSpawnerBlock.VoxelType.WATER)
    );
    
    public static final DeferredHolder<Block, VoxelSpawnerBlock> WIND_VOXEL_SPAWNER = BLOCKS.register(
        "wind_voxel_spawner",
        () -> new VoxelSpawnerBlock(BlockBehaviour.Properties.of()
            .strength(2.0f)
            .noOcclusion(), VoxelSpawnerBlock.VoxelType.WIND)
    );
    
    public static final DeferredHolder<Block, VoxelSpawnerBlock> STONE_VOXEL_SPAWNER = BLOCKS.register(
        "stone_voxel_spawner",
        () -> new VoxelSpawnerBlock(BlockBehaviour.Properties.of()
            .strength(2.0f)
            .noOcclusion(), VoxelSpawnerBlock.VoxelType.STONE)
    );
    
    public static final DeferredHolder<Block, VoxelSpawnerBlock> ICE_VOXEL_SPAWNER = BLOCKS.register(
        "ice_voxel_spawner",
        () -> new VoxelSpawnerBlock(BlockBehaviour.Properties.of()
            .strength(2.0f)
            .noOcclusion(), VoxelSpawnerBlock.VoxelType.ICE)
    );
    
    public static final DeferredHolder<Block, VoxelSpawnerBlock> LIGHTNING_VOXEL_SPAWNER = BLOCKS.register(
        "lightning_voxel_spawner",
        () -> new VoxelSpawnerBlock(BlockBehaviour.Properties.of()
            .strength(2.0f)
            .noOcclusion(), VoxelSpawnerBlock.VoxelType.LIGHTNING)
    );

    public static final DeferredHolder<Block, VoxelSpawnerBlock> POISON_VOXEL_SPAWNER = BLOCKS.register(
        "poison_voxel_spawner",
        () -> new VoxelSpawnerBlock(BlockBehaviour.Properties.of()
            .strength(2.0f)
            .noOcclusion(), VoxelSpawnerBlock.VoxelType.POISON)
    );
}


