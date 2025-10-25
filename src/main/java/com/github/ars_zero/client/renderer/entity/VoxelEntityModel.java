package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class VoxelEntityModel<T extends BaseVoxelEntity> extends GeoModel<T> {
    
    @Override
    public ResourceLocation getModelResource(T animatable) {
        return ArsZero.prefix("geo/voxel_entity.geo.json");
    }
    
    @Override
    public ResourceLocation getTextureResource(T animatable) {
        if (animatable instanceof com.github.ars_zero.common.entity.WaterVoxelEntity) {
            return ArsZero.prefix("textures/entity/water_voxel.png");
        } else if (animatable instanceof com.github.ars_zero.common.entity.FireVoxelEntity) {
            return ArsZero.prefix("textures/entity/fire_voxel.png");
        }
        return ArsZero.prefix("textures/entity/arcane_voxel.png");
    }
    
    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ArsZero.prefix("animations/voxel.animation.json");
    }
}

