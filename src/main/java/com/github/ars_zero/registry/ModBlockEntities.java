package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.block.BlightCauldronBlockEntity;
import com.github.ars_zero.common.block.MultiphaseSpellTurretTile;
import com.github.ars_zero.common.block.OssuaryBeaconBlockEntity;
import com.github.ars_zero.common.block.StaffDisplayBlockEntity;
import com.github.ars_zero.common.block.VoxelSpawnerBlockEntity;
import com.github.ars_zero.common.block.tile.BoneChestBlockEntity;
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
            ModBlocks.STONE_VOXEL_SPAWNER.get(),
            ModBlocks.ICE_VOXEL_SPAWNER.get(),
            ModBlocks.LIGHTNING_VOXEL_SPAWNER.get(),
            ModBlocks.BLIGHT_VOXEL_SPAWNER.get()
        ).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MultiphaseSpellTurretTile>> MULTIPHASE_SPELL_TURRET = BLOCK_ENTITIES.register(
        "multiphase_spell_turret",
        () -> {
            ArsZero.LOGGER.debug("Registering Multiphase Spell Turret block entity type");
            BlockEntityType<MultiphaseSpellTurretTile> type = BlockEntityType.Builder.of(
                MultiphaseSpellTurretTile::new,
                ModBlocks.MULTIPHASE_SPELL_TURRET.get()
            ).build(null);
            ArsZero.LOGGER.debug("Multiphase Spell Turret block entity type created successfully");
            return type;
        }
    );
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlightCauldronBlockEntity>> BLIGHT_CAULDRON = BLOCK_ENTITIES.register(
        "blight_cauldron",
        () -> BlockEntityType.Builder.of(
            BlightCauldronBlockEntity::new,
            ModBlocks.BLIGHT_CAULDRON.get()
        ).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StaffDisplayBlockEntity>> STAFF_DISPLAY = BLOCK_ENTITIES.register(
        "staff_display",
        () -> BlockEntityType.Builder.of(
            StaffDisplayBlockEntity::new,
            ModBlocks.STAFF_DISPLAY.get()
        ).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoneChestBlockEntity>> BONE_CHEST = BLOCK_ENTITIES.register(
        "bone_chest",
        () -> BlockEntityType.Builder.of(BoneChestBlockEntity::new, ModBlocks.BONE_CHEST.get()).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OssuaryBeaconBlockEntity>> OSSUARY_BEACON = BLOCK_ENTITIES.register(
        "ossuary_beacon",
        () -> BlockEntityType.Builder.of(
            OssuaryBeaconBlockEntity::new,
            ModBlocks.OSSUARY_BEACON.get()
        ).build(null)
    );
}
