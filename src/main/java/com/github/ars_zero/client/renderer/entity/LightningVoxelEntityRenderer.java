package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.client.renderer.ArsZeroRenderTypes;
import com.github.ars_zero.client.renderer.ArsZeroShaders;
import com.github.ars_zero.common.entity.LightningVoxelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class LightningVoxelEntityRenderer extends VoxelAnimatedRenderer<LightningVoxelEntity> {
    
    public LightningVoxelEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public RenderType getRenderType(LightningVoxelEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        var shader = ArsZeroShaders.ANIMATED_VOXEL;
        if (shader != null && Minecraft.getInstance().level != null) {
            shader.apply();
            float time = (Minecraft.getInstance().level.getGameTime() + partialTick) / 20.0f;
            var timeUniform = shader.getUniform("Time");
            if (timeUniform != null) {
                timeUniform.set(time);
            }
            
            float size = animatable.getSize();
            float scale = size / com.github.ars_zero.common.entity.BaseVoxelEntity.DEFAULT_BASE_SIZE;
            var scaleUniform = shader.getUniform("VoxelScale");
            if (scaleUniform != null) {
                scaleUniform.set(scale);
            }
        }
        
        return ArsZeroRenderTypes.animatedVoxelOpaque(texture);
    }
    
    
    @Override
    public void actuallyRender(PoseStack poseStack, LightningVoxelEntity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        int alpha = 255;
        int finalColor = 0xFFFF00 | (alpha << 24);
        
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, finalColor);
    }
}
