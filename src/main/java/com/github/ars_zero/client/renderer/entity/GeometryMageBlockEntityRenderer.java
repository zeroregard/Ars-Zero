package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.client.renderer.entity.model.GeometryMageBlockEntityModel;
import com.github.ars_zero.common.entity.mageblock.GeometryMageBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.util.Color;

public class GeometryMageBlockEntityRenderer
        extends AbstractGeometryEntityRenderer<GeometryMageBlockEntity> {

    public GeometryMageBlockEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new GeometryMageBlockEntityModel());
    }

    @Override
    public RenderType getRenderType(GeometryMageBlockEntity animatable, ResourceLocation texture,
            MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentEmissive(texture, false);
    }

    @Override
    public Color getRenderColor(GeometryMageBlockEntity animatable, float partialTick, int packedLight) {
        float r = animatable.getColorR();
        float g = animatable.getColorG();
        float b = animatable.getColorB();
        return Color.ofRGBA(r, g, b, 1.0f);
    }
}

