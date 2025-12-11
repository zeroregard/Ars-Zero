package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ArcaneShieldEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ArcaneShieldEntityModel extends GeoModel<ArcaneShieldEntity> {
    
    @Override
    public ResourceLocation getModelResource(ArcaneShieldEntity animatable) {
        return ArsZero.prefix("geo/arcane_shield.geo.json");
    }
    
    @Override
    public ResourceLocation getTextureResource(ArcaneShieldEntity animatable) {
        return ArsZero.prefix("textures/entity/arcane_shield.png");
    }
    
    @Override
    public ResourceLocation getAnimationResource(ArcaneShieldEntity animatable) {
        return ArsZero.prefix("animations/arcane_shield.animation.json");
    }
}
