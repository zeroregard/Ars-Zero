package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.ArcaneShieldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ArcaneShieldEntityRenderer extends EntityRenderer<ArcaneShieldEntity> {
    
    private static final float OUTLINE_RED = 0.54f;
    private static final float OUTLINE_GREEN = 0.17f;
    private static final float OUTLINE_BLUE = 0.89f;
    private static final float WIDTH = 1.0f;
    private static final float HEIGHT = 1.0f;
    private static final float DEPTH = 0.25f;
    
    public ArcaneShieldEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }
    
    @Override
    public void render(ArcaneShieldEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
        
        VertexConsumer outlineConsumer = buffer.getBuffer(RenderType.lines());
        renderCuboidOutline(poseStack, outlineConsumer, -WIDTH / 2.0, -HEIGHT / 2.0, -DEPTH / 2.0, WIDTH / 2.0, HEIGHT / 2.0, DEPTH / 2.0);
        
        poseStack.popPose();
    }
    
    private void renderCuboidOutline(PoseStack poseStack, VertexConsumer outlineConsumer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        LevelRenderer.renderLineBox(
            poseStack,
            outlineConsumer,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            OUTLINE_RED,
            OUTLINE_GREEN,
            OUTLINE_BLUE,
            1.0f
        );
    }
    
    @Override
    public ResourceLocation getTextureLocation(ArcaneShieldEntity entity) {
        return null;
    }
}
