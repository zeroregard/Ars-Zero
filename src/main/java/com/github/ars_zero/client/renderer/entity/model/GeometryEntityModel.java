package com.github.ars_zero.client.renderer.entity.model;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.GeometryEntity;
import net.minecraft.resources.ResourceLocation;

public class GeometryEntityModel extends GeometryProcessEntityModel<GeometryEntity> {
    private static final ResourceLocation TEXTURE = ArsZero.prefix("textures/entity/arcane_golem.png");

    @Override
    public ResourceLocation getTextureResource(GeometryEntity animatable) {
        return TEXTURE;
    }
}

