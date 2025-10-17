package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BaseVoxelEntityRenderer<T extends BaseVoxelEntity> extends GeoEntityRenderer<T> {
    
    public BaseVoxelEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new VoxelEntityModel<>());
    }
    
    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        float size = animatable.getSize();
        float scale = size / 0.25f;
        poseStack.scale(scale, scale, scale);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }
    
    @Override
    public void actuallyRender(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        int entityColor = animatable.getColor();
        int alpha = (int)(0.3f * 255);
        int finalColor = entityColor | (alpha << 24);
        
        RenderType actualRenderType = animatable.isEmissive()
            ? RenderType.entityTranslucentEmissive(getTextureLocation(animatable), false)
            : RenderType.entityTranslucent(getTextureLocation(animatable));
        
        super.actuallyRender(poseStack, animatable, model, actualRenderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, finalColor);
    }
}
