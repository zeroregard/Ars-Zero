package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.client.renderer.ArsZeroRenderTypes;
import com.github.ars_zero.client.renderer.ArsZeroShaders;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class VoxelAnimatedRenderer<T extends BaseVoxelEntity> extends VoxelBaseRenderer<T> {
    
    public VoxelAnimatedRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        var shader = ArsZeroShaders.ANIMATED_VOXEL;
        if (shader != null && Minecraft.getInstance().level != null) {
            shader.apply();
            float time = (Minecraft.getInstance().level.getGameTime() + partialTick) / 20.0f;
            var uniform = shader.getUniform("Time");
            if (uniform != null) {
                uniform.set(time);
            }
        }
        
        return ArsZeroRenderTypes.animatedVoxel(texture);
    }
}
