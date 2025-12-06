package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.client.renderer.ArsZeroRenderTypes;
import com.github.ars_zero.common.entity.BlightVoxelEntity;
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
        return ArsZeroRenderTypes.animatedVoxelOpaque(texture);
    }
}

