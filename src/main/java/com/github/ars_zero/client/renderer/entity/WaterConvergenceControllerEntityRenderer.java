package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.water.WaterConvergenceControllerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class WaterConvergenceControllerEntityRenderer extends EntityRenderer<WaterConvergenceControllerEntity> {

    public WaterConvergenceControllerEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }

    @Override
    public void render(WaterConvergenceControllerEntity entity, float entityYaw, float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WaterConvergenceControllerEntity entity) {
        return null;
    }
}
