package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.WindVoxelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class WindVoxelEntityRenderer extends VoxelAnimatedRenderer<WindVoxelEntity> {
    
    public WindVoxelEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public void actuallyRender(PoseStack poseStack, WindVoxelEntity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        int alpha = 255;
        int finalColor = 0xFFFFFF | (alpha << 24);
        
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, finalColor);
    }
}

