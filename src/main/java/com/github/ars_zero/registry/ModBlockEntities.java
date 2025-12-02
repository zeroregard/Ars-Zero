package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.block.VoxelSpawnerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ArsZero.MOD_ID);
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VoxelSpawnerBlockEntity>> VOXEL_SPAWNER = BLOCK_ENTITIES.register(
        "voxel_spawner",
        () -> BlockEntityType.Builder.of(
            VoxelSpawnerBlockEntity::new,
            ModBlocks.ARCANE_VOXEL_SPAWNER.get(),
            ModBlocks.FIRE_VOXEL_SPAWNER.get(),
            ModBlocks.WATER_VOXEL_SPAWNER.get(),
            ModBlocks.WIND_VOXEL_SPAWNER.get(),
            ModBlocks.STONE_VOXEL_SPAWNER.get()
        ).build(null)
    );
}


