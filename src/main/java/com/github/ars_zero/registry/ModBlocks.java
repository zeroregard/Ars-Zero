package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.block.BlightCauldronBlock;
import com.github.ars_zero.common.block.FrozenBlightBlock;
import com.github.ars_zero.common.block.MultiphaseSpellTurret;
import com.github.ars_zero.common.block.StaffDisplayBlock;
import com.github.ars_zero.common.block.VoxelSpawnerBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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

    public static final DeferredHolder<Block, VoxelSpawnerBlock> BLIGHT_VOXEL_SPAWNER = BLOCKS.register(
        "blight_voxel_spawner",
        () -> new VoxelSpawnerBlock(BlockBehaviour.Properties.of()
            .strength(2.0f)
            .noOcclusion(), VoxelSpawnerBlock.VoxelType.BLIGHT)
    );

    public static final DeferredHolder<Block, MultiphaseSpellTurret> MULTIPHASE_SPELL_TURRET = BLOCKS.register(
        "multiphase_spell_turret",
        () -> {
            ArsZero.LOGGER.debug("Registering Multiphase Spell Turret block");
            MultiphaseSpellTurret block = new MultiphaseSpellTurret(BlockBehaviour.Properties.of()
                .strength(2.0f)
                .noOcclusion());
            ArsZero.LOGGER.debug("Multiphase Spell Turret block created successfully");
            return block;
        }
    );
    
    public static final DeferredHolder<Block, BlightCauldronBlock> BLIGHT_CAULDRON = BLOCKS.register(
        "blight_cauldron",
        () -> new BlightCauldronBlock(BlockBehaviour.Properties.of()
            .strength(2.0f)
            .noOcclusion())
    );
    
    public static final DeferredHolder<Block, FrozenBlightBlock> FROZEN_BLIGHT = BLOCKS.register(
        "frozen_blight",
        () -> new FrozenBlightBlock(BlockBehaviour.Properties.of()
            .strength(0.5f)
            .sound(SoundType.GLASS))
    );

    public static final DeferredHolder<Block, StaffDisplayBlock> STAFF_DISPLAY = BLOCKS.register(
        "staff_display",
        StaffDisplayBlock::new
    );

    /** Dead archwood log for blight forest; no leaves. Same shape as Ars Nouveau archwood trees but blighted. */
    public static final BlockBehaviour.Properties BLIGHT_LOG_PROP = BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_GRAY)
        .strength(2.0f, 3.0f)
        .ignitedByLava()
        .sound(SoundType.WOOD);

    public static final DeferredHolder<Block, RotatedPillarBlock> BLIGHT_ARCHWOOD_LOG = BLOCKS.register(
        "blight_archwood_log",
        () -> new RotatedPillarBlock(BLIGHT_LOG_PROP)
    );
}
