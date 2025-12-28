package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.ExplosionControllerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ExplosionControllerEntityRenderer extends EntityRenderer<ExplosionControllerEntity> {
    
    private static final float SIZE = 0.5f;
    // White (no charge) -> Red (fully charged)
    private static final float NO_CHARGE_RED = 1.0f;
    private static final float NO_CHARGE_GREEN = 1.0f;
    private static final float NO_CHARGE_BLUE = 1.0f;
    private static final float FULL_CHARGE_RED = 1.0f;
    private static final float FULL_CHARGE_GREEN = 0.0f;
    private static final float FULL_CHARGE_BLUE = 0.0f;
    private static final float ALPHA = 0.8f;
    
    public ExplosionControllerEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }
    
    @Override
    public void render(ExplosionControllerEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        float charge = entity.getCharge();
        
        // Interpolate from white (no charge) to red (fully charged)
        float red = interpolateColor(NO_CHARGE_RED, FULL_CHARGE_RED, charge);
        float green = interpolateColor(NO_CHARGE_GREEN, FULL_CHARGE_GREEN, charge);
        float blue = interpolateColor(NO_CHARGE_BLUE, FULL_CHARGE_BLUE, charge);
        
        float halfSize = SIZE / 2.0f;
        VertexConsumer outlineConsumer = buffer.getBuffer(RenderType.lines());
        
        LevelRenderer.renderLineBox(
            poseStack,
            outlineConsumer,
            -halfSize,
            -halfSize,
            -halfSize,
            halfSize,
            halfSize,
            halfSize,
            red,
            green,
            blue,
            ALPHA
        );
        
        poseStack.popPose();
    }

    private float interpolateColor(float start, float end, float t) {
        return start + (end - start) * Math.min(1.0f, Math.max(0.0f, t));
    }
    
    @Override
    public ResourceLocation getTextureLocation(ExplosionControllerEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/white_concrete.png");
    }
}

