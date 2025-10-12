package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.VoxelEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ArsZero.MOD_ID);
    
    public static final DeferredHolder<EntityType<?>, EntityType<VoxelEntity>> VOXEL_ENTITY = ENTITIES.register(
            "voxel_entity",
            () -> EntityType.Builder.<VoxelEntity>of(VoxelEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .setShouldReceiveVelocityUpdates(false)
                    .noSave()
                    .build(ArsZero.MOD_ID + ":voxel_entity")
    );
}
