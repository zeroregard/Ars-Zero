package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ArcaneVoxelEntity;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.common.entity.ExplosionControllerEntity;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.IceVoxelEntity;
import com.github.ars_zero.common.entity.LightningVoxelEntity;
import com.github.ars_zero.common.entity.StoneVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.common.entity.WindVoxelEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ArsZero.MOD_ID);
    
    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneVoxelEntity>> ARCANE_VOXEL_ENTITY = ENTITIES.register(
            "arcane_voxel_entity",
            () -> EntityType.Builder.<ArcaneVoxelEntity>of(ArcaneVoxelEntity::new, MobCategory.MISC)
                    .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE, BaseVoxelEntity.DEFAULT_BASE_SIZE)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ArsZero.MOD_ID + ":arcane_voxel_entity")
    );
    
    public static final DeferredHolder<EntityType<?>, EntityType<WaterVoxelEntity>> WATER_VOXEL_ENTITY = ENTITIES.register(
            "water_voxel_entity",
            () -> EntityType.Builder.<WaterVoxelEntity>of(WaterVoxelEntity::new, MobCategory.MISC)
                    .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE, BaseVoxelEntity.DEFAULT_BASE_SIZE)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ArsZero.MOD_ID + ":water_voxel_entity")
    );
    
    public static final DeferredHolder<EntityType<?>, EntityType<FireVoxelEntity>> FIRE_VOXEL_ENTITY = ENTITIES.register(
            "fire_voxel_entity",
            () -> EntityType.Builder.<FireVoxelEntity>of(FireVoxelEntity::new, MobCategory.MISC)
                    .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE, BaseVoxelEntity.DEFAULT_BASE_SIZE)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ArsZero.MOD_ID + ":fire_voxel_entity")
    );
    
    public static final DeferredHolder<EntityType<?>, EntityType<StoneVoxelEntity>> STONE_VOXEL_ENTITY = ENTITIES.register(
            "stone_voxel_entity",
            () -> EntityType.Builder.<StoneVoxelEntity>of(StoneVoxelEntity::new, MobCategory.MISC)
                    .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE, BaseVoxelEntity.DEFAULT_BASE_SIZE)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ArsZero.MOD_ID + ":stone_voxel_entity")
    );
    
    public static final DeferredHolder<EntityType<?>, EntityType<WindVoxelEntity>> WIND_VOXEL_ENTITY = ENTITIES.register(
            "wind_voxel_entity",
            () -> EntityType.Builder.<WindVoxelEntity>of(WindVoxelEntity::new, MobCategory.MISC)
                    .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE, BaseVoxelEntity.DEFAULT_BASE_SIZE)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ArsZero.MOD_ID + ":wind_voxel_entity")
    );
    
    public static final DeferredHolder<EntityType<?>, EntityType<IceVoxelEntity>> ICE_VOXEL_ENTITY = ENTITIES.register(
            "ice_voxel_entity",
            () -> EntityType.Builder.<IceVoxelEntity>of(IceVoxelEntity::new, MobCategory.MISC)
                    .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE, BaseVoxelEntity.DEFAULT_BASE_SIZE)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ArsZero.MOD_ID + ":ice_voxel_entity")
    );
    
    public static final DeferredHolder<EntityType<?>, EntityType<LightningVoxelEntity>> LIGHTNING_VOXEL_ENTITY = ENTITIES.register(
            "lightning_voxel_entity",
            () -> EntityType.Builder.<LightningVoxelEntity>of(LightningVoxelEntity::new, MobCategory.MISC)
                    .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE, BaseVoxelEntity.DEFAULT_BASE_SIZE)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ArsZero.MOD_ID + ":lightning_voxel_entity")
    );
    
    public static final DeferredHolder<EntityType<?>, EntityType<BlockGroupEntity>> BLOCK_GROUP = ENTITIES.register(
            "block_group",
            () -> EntityType.Builder.<BlockGroupEntity>of(BlockGroupEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ArsZero.MOD_ID + ":block_group")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<ExplosionControllerEntity>> EXPLOSION_CONTROLLER = ENTITIES.register(
            "explosion_controller",
            () -> EntityType.Builder.<ExplosionControllerEntity>of(ExplosionControllerEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .clientTrackingRange(1)
                    .updateInterval(20)
                    .setShouldReceiveVelocityUpdates(false)
                    .build(ArsZero.MOD_ID + ":explosion_controller")
    );
}
