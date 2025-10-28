package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VoxelBaseRenderer<T extends BaseVoxelEntity> extends GeoEntityRenderer<T> {
    
    public VoxelBaseRenderer(EntityRendererProvider.Context context) {
        super(context, new VoxelEntityModel<>());
    }
    
    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return animatable.isEmissive()
            ? RenderType.entityTranslucentEmissive(texture, false)
            : RenderType.entityTranslucent(texture);
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
        int alpha = 128;
        
        if (animatable instanceof com.github.ars_zero.common.entity.CompressibleEntity compressible) {
            float compressionLevel = compressible.getCompressionLevel();
            float emissiveIntensity = compressible.getEmissiveIntensity();
            
            alpha = (int) (128 + 127 * compressionLevel);
            
            if (emissiveIntensity > 0.5f) {
                alpha = 255;
            }
        }
        
        int finalColor = entityColor | (alpha << 24);
        
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, finalColor);
    }
}