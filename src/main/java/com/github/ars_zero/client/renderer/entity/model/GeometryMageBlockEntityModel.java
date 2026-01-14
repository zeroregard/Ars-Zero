package com.github.ars_zero.client.renderer.entity.model;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.mageblock.GeometryMageBlockEntity;
import net.minecraft.resources.ResourceLocation;

public class GeometryMageBlockEntityModel extends GeometryConvergenceEntityModel<GeometryMageBlockEntity> {
    private static final ResourceLocation TEXTURE = ArsZero.prefix("textures/entity/arcane_golem.png");

    @Override
    public ResourceLocation getTextureResource(GeometryMageBlockEntity animatable) {
        return TEXTURE;
    }
}

