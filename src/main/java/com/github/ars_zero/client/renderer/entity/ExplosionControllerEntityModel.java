package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ExplosionControllerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ExplosionControllerEntityModel extends GeoModel<ExplosionControllerEntity> {
    
    @Override
    public ResourceLocation getModelResource(ExplosionControllerEntity animatable) {
        return ArsZero.prefix("geo/explosion_charging.geo.json");
    }
    
    @Override
    public ResourceLocation getTextureResource(ExplosionControllerEntity animatable) {
        return ArsZero.prefix("textures/entity/explosion_charging.png");
    }
    
    @Override
    public ResourceLocation getAnimationResource(ExplosionControllerEntity animatable) {
        return ArsZero.prefix("animations/explosion_charging.animations.json");
    }
}

