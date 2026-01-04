package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.water.WaterConvergenceControllerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class WaterConvergenceControllerEntityRenderer extends EntityRenderer<WaterConvergenceControllerEntity> {

    private static final float OUTLINE_RED = 0.1f;
    private static final float OUTLINE_GREEN = 0.4f;
    private static final float OUTLINE_BLUE = 1.0f;

    public WaterConvergenceControllerEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }

    @Override
    public void render(WaterConvergenceControllerEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        VertexConsumer outline = buffer.getBuffer(RenderType.lines());
        double half = 0.5;
        LevelRenderer.renderLineBox(
                poseStack,
                outline,
                -half,
                -half,
                -half,
                half,
                half,
                half,
                OUTLINE_RED,
                OUTLINE_GREEN,
                OUTLINE_BLUE,
                1.0f
        );
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(WaterConvergenceControllerEntity entity) {
        return null;
    }
}

