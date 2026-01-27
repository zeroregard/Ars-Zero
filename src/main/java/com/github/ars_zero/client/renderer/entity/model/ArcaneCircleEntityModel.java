package com.github.ars_zero.client.renderer.entity.model;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ArcaneCircleEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ArcaneCircleEntityModel extends GeoModel<ArcaneCircleEntity> {
    private static final ResourceLocation MODEL = ArsZero.prefix("geo/arcane_circle.geo.json");
    private static final ResourceLocation TEXTURE = ArsZero.prefix("textures/entity/arcane_circle.png");
    private static final ResourceLocation ANIMATION = ArsZero.prefix("animations/arcane_circle.animation.json");

    @Override
    public ResourceLocation getModelResource(ArcaneCircleEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ArcaneCircleEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ArcaneCircleEntity animatable) {
        return ANIMATION;
    }
}
