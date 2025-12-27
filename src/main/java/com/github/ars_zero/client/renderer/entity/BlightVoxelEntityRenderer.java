package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.client.renderer.ArsZeroRenderTypes;
import com.github.ars_zero.client.renderer.ArsZeroShaders;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.BlightVoxelEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BlightVoxelEntityRenderer extends VoxelAnimatedRenderer<BlightVoxelEntity> {
    public BlightVoxelEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public RenderType getRenderType(BlightVoxelEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        var shader = ArsZeroShaders.ANIMATED_VOXEL;
        if (shader != null && Minecraft.getInstance().level != null) {
            shader.apply();
            float time = (Minecraft.getInstance().level.getGameTime() + partialTick) / 20.0f;
            var timeUniform = shader.getUniform("Time");
            if (timeUniform != null) {
                timeUniform.set(time);
            }
            
            float size = animatable.getSize();
            float scale = size / BaseVoxelEntity.DEFAULT_BASE_SIZE;
            var scaleUniform = shader.getUniform("VoxelScale");
            if (scaleUniform != null) {
                scaleUniform.set(scale);
            }
        }
        
        return ArsZeroRenderTypes.animatedVoxelOpaque(texture);
    }
}

