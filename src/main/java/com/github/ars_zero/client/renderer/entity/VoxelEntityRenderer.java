package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.VoxelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

public class VoxelEntityRenderer extends EntityRenderer<VoxelEntity> {
    
    public VoxelEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(VoxelEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        
        float size = entity.getSize();
        
        poseStack.pushPose();
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.LINES);
        
        float halfSize = size / 2.0f;
        
        // Bottom face
        drawLine(vertexConsumer, poseStack, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize);
        drawLine(vertexConsumer, poseStack, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize);
        drawLine(vertexConsumer, poseStack, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize);
        drawLine(vertexConsumer, poseStack, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, -halfSize);
        
        // Top face
        drawLine(vertexConsumer, poseStack, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize);
        drawLine(vertexConsumer, poseStack, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize);
        drawLine(vertexConsumer, poseStack, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize);
        drawLine(vertexConsumer, poseStack, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize);
        
        // Vertical edges
        drawLine(vertexConsumer, poseStack, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize);
        drawLine(vertexConsumer, poseStack, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize);
        drawLine(vertexConsumer, poseStack, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize);
        drawLine(vertexConsumer, poseStack, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize);
        
        poseStack.popPose();
    }
    
    private void drawLine(VertexConsumer consumer, PoseStack poseStack, float x1, float y1, float z1, float x2, float y2, float z2) {
        consumer.addVertex(poseStack.last(), x1, y1, z1)
                .setColor(0.6f, 0.0f, 1.0f, 1.0f)
                .setNormal(poseStack.last(), 0, 1, 0);
        consumer.addVertex(poseStack.last(), x2, y2, z2)
                .setColor(0.6f, 0.0f, 1.0f, 1.0f)
                .setNormal(poseStack.last(), 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(VoxelEntity entity) {
        return null; // No texture needed for wireframe
    }
}
