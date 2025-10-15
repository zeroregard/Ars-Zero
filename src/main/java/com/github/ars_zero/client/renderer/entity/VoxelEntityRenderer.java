package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.VoxelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class VoxelEntityRenderer extends EntityRenderer<VoxelEntity> {
    
    private static final ResourceLocation TEXTURE = ArsZero.prefix("textures/entity/arcane_voxel.png");
    private static final float BASE_SIZE = 0.25f;
    
    public VoxelEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(VoxelEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        
        float size = entity.getSize();
        
        poseStack.pushPose();
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE, false));
        Matrix4f matrix = poseStack.last().pose();
        
        float halfSize = size / 2.0f;
        float uvScale = size / BASE_SIZE;
        
        // Draw all 6 faces with tiling UVs
        // Bottom face (Y-)
        drawQuad(vertexConsumer, matrix, 
            -halfSize, -halfSize, -halfSize, 0, 0,
            halfSize, -halfSize, -halfSize, uvScale, 0,
            halfSize, -halfSize, halfSize, uvScale, uvScale,
            -halfSize, -halfSize, halfSize, 0, uvScale,
            0, -1, 0, packedLight);
        
        // Top face (Y+)
        drawQuad(vertexConsumer, matrix,
            -halfSize, halfSize, halfSize, 0, 0,
            halfSize, halfSize, halfSize, uvScale, 0,
            halfSize, halfSize, -halfSize, uvScale, uvScale,
            -halfSize, halfSize, -halfSize, 0, uvScale,
            0, 1, 0, packedLight);
        
        // North face (Z-)
        drawQuad(vertexConsumer, matrix,
            -halfSize, -halfSize, -halfSize, 0, 0,
            -halfSize, halfSize, -halfSize, 0, uvScale,
            halfSize, halfSize, -halfSize, uvScale, uvScale,
            halfSize, -halfSize, -halfSize, uvScale, 0,
            0, 0, -1, packedLight);
        
        // South face (Z+)
        drawQuad(vertexConsumer, matrix,
            halfSize, -halfSize, halfSize, 0, 0,
            halfSize, halfSize, halfSize, 0, uvScale,
            -halfSize, halfSize, halfSize, uvScale, uvScale,
            -halfSize, -halfSize, halfSize, uvScale, 0,
            0, 0, 1, packedLight);
        
        // West face (X-)
        drawQuad(vertexConsumer, matrix,
            -halfSize, -halfSize, halfSize, 0, 0,
            -halfSize, halfSize, halfSize, 0, uvScale,
            -halfSize, halfSize, -halfSize, uvScale, uvScale,
            -halfSize, -halfSize, -halfSize, uvScale, 0,
            -1, 0, 0, packedLight);
        
        // East face (X+)
        drawQuad(vertexConsumer, matrix,
            halfSize, -halfSize, -halfSize, 0, 0,
            halfSize, halfSize, -halfSize, 0, uvScale,
            halfSize, halfSize, halfSize, uvScale, uvScale,
            halfSize, -halfSize, halfSize, uvScale, 0,
            1, 0, 0, packedLight);
        
        poseStack.popPose();
    }
    
    private void drawQuad(VertexConsumer consumer, Matrix4f matrix,
                         float x1, float y1, float z1, float u1, float v1,
                         float x2, float y2, float z2, float u2, float v2,
                         float x3, float y3, float z3, float u3, float v3,
                         float x4, float y4, float z4, float u4, float v4,
                         float nx, float ny, float nz, int light) {
        int alpha = (int)(255 * 0.3f);
        consumer.addVertex(matrix, x1, y1, z1).setColor(255, 255, 255, alpha).setUv(u1, v1).setOverlay(0).setLight(15728640).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x2, y2, z2).setColor(255, 255, 255, alpha).setUv(u2, v2).setOverlay(0).setLight(15728640).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x3, y3, z3).setColor(255, 255, 255, alpha).setUv(u3, v3).setOverlay(0).setLight(15728640).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x4, y4, z4).setColor(255, 255, 255, alpha).setUv(u4, v4).setOverlay(0).setLight(15728640).setNormal(nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(VoxelEntity entity) {
        return TEXTURE;
    }
}
