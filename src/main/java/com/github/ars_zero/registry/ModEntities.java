package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ArcaneVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
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
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .noSave()
                    .build(ArsZero.MOD_ID + ":arcane_voxel_entity")
    );
    
    public static final DeferredHolder<EntityType<?>, EntityType<WaterVoxelEntity>> WATER_VOXEL_ENTITY = ENTITIES.register(
            "water_voxel_entity",
            () -> EntityType.Builder.<WaterVoxelEntity>of(WaterVoxelEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .noSave()
                    .build(ArsZero.MOD_ID + ":water_voxel_entity")
    );
}
