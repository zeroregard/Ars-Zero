package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.client.renderer.entity.model.GeometryEntityModel;
import com.github.ars_zero.common.entity.GeometryEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.util.Color;

public class GeometryEntityRenderer
        extends AbstractGeometryEntityRenderer<GeometryEntity> {

    public GeometryEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new GeometryEntityModel());
    }

    @Override
    public RenderType getRenderType(GeometryEntity animatable, ResourceLocation texture,
            MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentEmissive(texture, false);
    }

    @Override
    public Color getRenderColor(GeometryEntity animatable, float partialTick, int packedLight) {
        float r = animatable.getColorR();
        float g = animatable.getColorG();
        float b = animatable.getColorB();
        return Color.ofRGBA(r, g, b, 1.0f);
    }
}

