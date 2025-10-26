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
            var timeUniform = shader.getUniform("Time");
            if (timeUniform != null) {
                timeUniform.set(time);
            }
            
            if (animatable instanceof com.github.ars_zero.common.entity.CompressibleVoxelEntity compressible) {
                var compressionUniform = shader.getUniform("CompressionLevel");
                if (compressionUniform != null) {
                    compressionUniform.set(compressible.getCompressionLevel());
                }
                
                var emissiveUniform = shader.getUniform("EmissiveIntensity");
                if (emissiveUniform != null) {
                    emissiveUniform.set(compressible.getEmissiveIntensity());
                }
            }
        }
        
        return ArsZeroRenderTypes.animatedVoxel(texture);
    }
}
