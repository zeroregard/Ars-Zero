package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.block.PhasedSpellTurret;
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

    public static final DeferredHolder<Block, PhasedSpellTurret> PHASED_SPELL_TURRET = BLOCKS.register(
        "phased_spell_turret",
        () -> new PhasedSpellTurret(BlockBehaviour.Properties.of()
            .strength(2.0f)
            .noOcclusion())
    );
}


