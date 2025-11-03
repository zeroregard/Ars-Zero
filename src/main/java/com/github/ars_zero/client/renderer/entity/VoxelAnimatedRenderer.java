package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.client.renderer.ArsZeroRenderTypes;
import com.github.ars_zero.client.renderer.ArsZeroShaders;
import com.github.ars_zero.client.renderer.RibbonTrailRenderer;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class VoxelAnimatedRenderer<T extends BaseVoxelEntity> extends VoxelBaseRenderer<T> {
    
    public VoxelAnimatedRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        float compressionLevel = 0.0f;
        float emissiveIntensity = 0.0f;
        
        if (animatable instanceof com.github.ars_zero.common.entity.CompressibleEntity compressible) {
            compressionLevel = compressible.getCompressionLevel();
            emissiveIntensity = compressible.getEmissiveIntensity();
        }
        
        boolean isEmissive = compressionLevel > 0.5f;
        
        RenderType renderType = ArsZeroRenderTypes.animatedVoxel(texture, isEmissive);
        
        var shader = ArsZeroShaders.ANIMATED_VOXEL;
        
        if (shader != null && Minecraft.getInstance().level != null) {
            float time = (Minecraft.getInstance().level.getGameTime() + partialTick) / 20.0f;
            var timeUniform = shader.getUniform("Time");
            if (timeUniform != null) {
                timeUniform.set(time);
            }
            
            float size = animatable.getSize();
            float scale = size / 0.25f;
            var scaleUniform = shader.getUniform("VoxelScale");
            if (scaleUniform != null) {
                scaleUniform.set(scale);
            }
            
            var compressionUniform = shader.getUniform("CompressionLevel");
            if (compressionUniform != null) {
                compressionUniform.set(compressionLevel);
            }
            
            var emissiveUniform = shader.getUniform("EmissiveIntensity");
            if (emissiveUniform != null) {
                emissiveUniform.set(emissiveIntensity);
            }
        }
        
        return renderType;
    }
    
    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity instanceof com.github.ars_zero.common.entity.CompressibleEntity compressible 
            && compressible.getCompressionLevel() > 0.5f) {
            
            java.util.List<Vec3> trailPoints = entity.getTrailPositions();
            
            if (!trailPoints.isEmpty()) {
                poseStack.pushPose();
                
                Vec3 entityRenderPos = entity.position();
                poseStack.translate(-entityRenderPos.x, -entityRenderPos.y, -entityRenderPos.z);
                
                float width = entity.getSize() * 0.8f;
                int color = compressible.getCompressedColor();
                float alpha = Math.min(1.0f, compressible.getCompressionLevel() * 1.5f);
                
                RibbonTrailRenderer.renderTrail(trailPoints, poseStack, bufferSource, packedLight, width, color, alpha);
                
                poseStack.popPose();
            }
        }
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
    
    @Override
    public boolean shouldRender(T entity, net.minecraft.client.renderer.culling.Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }
}
