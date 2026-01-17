package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ArcaneVoxelEntity;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.common.entity.explosion.ExplosionControllerEntity;
import com.github.ars_zero.common.entity.explosion.ExplosionBurstProjectile;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.SourceJarChargerEntity;
import com.github.ars_zero.common.entity.PlayerChargerEntity;
import com.github.ars_zero.common.entity.water.WaterConvergenceControllerEntity;
import com.github.ars_zero.common.entity.IceVoxelEntity;
import com.github.ars_zero.common.entity.LightningVoxelEntity;
import com.github.ars_zero.common.entity.BlightVoxelEntity;
import com.github.ars_zero.common.entity.StoneVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.common.entity.WindVoxelEntity;
import com.github.ars_zero.common.entity.terrain.GeometryTerrainEntity;
import com.github.ars_zero.common.entity.break_blocks.GeometryBreakEntity;
import com.github.ars_zero.common.entity.GeometryEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

        public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister
                        .create(BuiltInRegistries.ENTITY_TYPE, ArsZero.MOD_ID);

        public static final DeferredHolder<EntityType<?>, EntityType<ArcaneVoxelEntity>> ARCANE_VOXEL_ENTITY = ENTITIES
                        .register(
                                        "arcane_voxel_entity",
                                        () -> EntityType.Builder
                                                        .<ArcaneVoxelEntity>of(ArcaneVoxelEntity::new, MobCategory.MISC)
                                                        .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE,
                                                                        BaseVoxelEntity.DEFAULT_BASE_SIZE)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(true)
                                                        .build(ArsZero.MOD_ID + ":arcane_voxel_entity"));

        public static final DeferredHolder<EntityType<?>, EntityType<WaterVoxelEntity>> WATER_VOXEL_ENTITY = ENTITIES
                        .register(
                                        "water_voxel_entity",
                                        () -> EntityType.Builder
                                                        .<WaterVoxelEntity>of(WaterVoxelEntity::new, MobCategory.MISC)
                                                        .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE,
                                                                        BaseVoxelEntity.DEFAULT_BASE_SIZE)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(true)
                                                        .build(ArsZero.MOD_ID + ":water_voxel_entity"));

        public static final DeferredHolder<EntityType<?>, EntityType<FireVoxelEntity>> FIRE_VOXEL_ENTITY = ENTITIES
                        .register(
                                        "fire_voxel_entity",
                                        () -> EntityType.Builder
                                                        .<FireVoxelEntity>of(FireVoxelEntity::new, MobCategory.MISC)
                                                        .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE,
                                                                        BaseVoxelEntity.DEFAULT_BASE_SIZE)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(true)
                                                        .build(ArsZero.MOD_ID + ":fire_voxel_entity"));

        public static final DeferredHolder<EntityType<?>, EntityType<StoneVoxelEntity>> STONE_VOXEL_ENTITY = ENTITIES
                        .register(
                                        "stone_voxel_entity",
                                        () -> EntityType.Builder
                                                        .<StoneVoxelEntity>of(StoneVoxelEntity::new, MobCategory.MISC)
                                                        .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE,
                                                                        BaseVoxelEntity.DEFAULT_BASE_SIZE)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(true)
                                                        .build(ArsZero.MOD_ID + ":stone_voxel_entity"));

        public static final DeferredHolder<EntityType<?>, EntityType<WindVoxelEntity>> WIND_VOXEL_ENTITY = ENTITIES
                        .register(
                                        "wind_voxel_entity",
                                        () -> EntityType.Builder
                                                        .<WindVoxelEntity>of(WindVoxelEntity::new, MobCategory.MISC)
                                                        .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE,
                                                                        BaseVoxelEntity.DEFAULT_BASE_SIZE)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(true)
                                                        .build(ArsZero.MOD_ID + ":wind_voxel_entity"));

        public static final DeferredHolder<EntityType<?>, EntityType<IceVoxelEntity>> ICE_VOXEL_ENTITY = ENTITIES
                        .register(
                                        "ice_voxel_entity",
                                        () -> EntityType.Builder
                                                        .<IceVoxelEntity>of(IceVoxelEntity::new, MobCategory.MISC)
                                                        .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE,
                                                                        BaseVoxelEntity.DEFAULT_BASE_SIZE)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(true)
                                                        .build(ArsZero.MOD_ID + ":ice_voxel_entity"));

        public static final DeferredHolder<EntityType<?>, EntityType<LightningVoxelEntity>> LIGHTNING_VOXEL_ENTITY = ENTITIES
                        .register(
                                        "lightning_voxel_entity",
                                        () -> EntityType.Builder
                                                        .<LightningVoxelEntity>of(LightningVoxelEntity::new,
                                                                        MobCategory.MISC)
                                                        .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE,
                                                                        BaseVoxelEntity.DEFAULT_BASE_SIZE)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(true)
                                                        .build(ArsZero.MOD_ID + ":lightning_voxel_entity"));

        public static final DeferredHolder<EntityType<?>, EntityType<BlightVoxelEntity>> BLIGHT_VOXEL_ENTITY = ENTITIES
                        .register(
                                        "blight_voxel_entity",
                                        () -> EntityType.Builder
                                                        .<BlightVoxelEntity>of(BlightVoxelEntity::new, MobCategory.MISC)
                                                        .sized(BaseVoxelEntity.DEFAULT_BASE_SIZE,
                                                                        BaseVoxelEntity.DEFAULT_BASE_SIZE)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(true)
                                                        .build(ArsZero.MOD_ID + ":blight_voxel_entity"));

        public static final DeferredHolder<EntityType<?>, EntityType<BlockGroupEntity>> BLOCK_GROUP = ENTITIES.register(
                        "block_group",
                        () -> EntityType.Builder.<BlockGroupEntity>of(BlockGroupEntity::new, MobCategory.MISC)
                                        .sized(1.0f, 1.0f)
                                        .clientTrackingRange(64)
                                        .updateInterval(1)
                                        .setShouldReceiveVelocityUpdates(true)
                                        .build(ArsZero.MOD_ID + ":block_group"));

        public static final DeferredHolder<EntityType<?>, EntityType<ExplosionControllerEntity>> EXPLOSION_CONTROLLER = ENTITIES
                        .register(
                                        "explosion_controller",
                                        () -> EntityType.Builder
                                                        .<ExplosionControllerEntity>of(ExplosionControllerEntity::new,
                                                                        MobCategory.MISC)
                                                        .sized(0.1f, 0.1f)
                                                        .clientTrackingRange(256)
                                                        .updateInterval(20)
                                                        .setShouldReceiveVelocityUpdates(false)
                                                        .build(ArsZero.MOD_ID + ":explosion_controller"));

        public static final DeferredHolder<EntityType<?>, EntityType<WaterConvergenceControllerEntity>> WATER_CONVERGENCE_CONTROLLER = ENTITIES
                        .register(
                                        "water_convergence_controller",
                                        () -> EntityType.Builder
                                                        .<WaterConvergenceControllerEntity>of(
                                                                        WaterConvergenceControllerEntity::new,
                                                                        MobCategory.MISC)
                                                        .sized(0.1f, 0.1f)
                                                        .clientTrackingRange(256)
                                                        .updateInterval(20)
                                                        .setShouldReceiveVelocityUpdates(false)
                                                        .build(ArsZero.MOD_ID + ":water_convergence_controller"));

        public static final DeferredHolder<EntityType<?>, EntityType<GeometryTerrainEntity>> GEOMETRY_TERRAIN_CONTROLLER = ENTITIES
                        .register(
                                        "geometry_terrain_controller",
                                        () -> EntityType.Builder
                                                        .<GeometryTerrainEntity>of(
                                                                        GeometryTerrainEntity::new,
                                                                        MobCategory.MISC)
                                                        .sized(0.1f, 0.1f)
                                                        .clientTrackingRange(256)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(true)
                                                        .build(ArsZero.MOD_ID
                                                                        + ":geometry_terrain_controller"));

        public static final DeferredHolder<EntityType<?>, EntityType<GeometryBreakEntity>> GEOMETRY_BREAK_CONTROLLER = ENTITIES
                        .register(
                                        "geometry_break_controller",
                                        () -> EntityType.Builder.<GeometryBreakEntity>of(
                                                        GeometryBreakEntity::new,
                                                        MobCategory.MISC)
                                                        .sized(0.1f, 0.1f)
                                                        .clientTrackingRange(256)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(true)
                                                        .build(ArsZero.MOD_ID + ":geometry_break_controller"));

        public static final DeferredHolder<EntityType<?>, EntityType<GeometryEntity>> GEOMETRY_CONTROLLER = ENTITIES
                        .register(
                                        "geometry_controller",
                                        () -> EntityType.Builder.<GeometryEntity>of(
                                                        GeometryEntity::new,
                                                        MobCategory.MISC)
                                                        .sized(0.1f, 0.1f)
                                                        .clientTrackingRange(256)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(true)
                                                        .build(ArsZero.MOD_ID + ":geometry_controller"));

        public static final DeferredHolder<EntityType<?>, EntityType<SourceJarChargerEntity>> SOURCE_JAR_CHARGER = ENTITIES
                        .register(
                                        "source_jar_charger",
                                        () -> EntityType.Builder
                                                        .<SourceJarChargerEntity>of(SourceJarChargerEntity::new,
                                                                        MobCategory.MISC)
                                                        .sized(0.1f, 0.1f)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(false)
                                                        .build(ArsZero.MOD_ID + ":source_jar_charger"));

        public static final DeferredHolder<EntityType<?>, EntityType<PlayerChargerEntity>> PLAYER_CHARGER = ENTITIES
                        .register(
                                        "player_charger",
                                        () -> EntityType.Builder
                                                        .<PlayerChargerEntity>of(PlayerChargerEntity::new,
                                                                        MobCategory.MISC)
                                                        .sized(0.1f, 0.1f)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(false)
                                                        .build(ArsZero.MOD_ID + ":player_charger"));

        public static final DeferredHolder<EntityType<?>, EntityType<ExplosionBurstProjectile>> EXPLOSION_BURST_PROJECTILE = ENTITIES
                        .register(
                                        "explosion_burst_projectile",
                                        () -> EntityType.Builder
                                                        .<ExplosionBurstProjectile>of(ExplosionBurstProjectile::new,
                                                                        MobCategory.MISC)
                                                        .sized(0.1f, 0.1f)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .setShouldReceiveVelocityUpdates(true)
                                                        .build(ArsZero.MOD_ID + ":explosion_burst_projectile"));
}
