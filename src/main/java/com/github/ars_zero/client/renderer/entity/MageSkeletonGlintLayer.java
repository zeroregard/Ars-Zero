package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.MageSkeletonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.monster.Skeleton;

/**
 * Renders the skeleton body model with the enchanted glint overlay (same as armor/item glint).
 * Only applied for MageSkeletonEntity; armor/held items are unchanged.
 */
public class MageSkeletonGlintLayer extends RenderLayer<Skeleton, SkeletonModel<Skeleton>> {

    public MageSkeletonGlintLayer(RenderLayerParent<Skeleton, SkeletonModel<Skeleton>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       Skeleton entity, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(entity instanceof MageSkeletonEntity)) {
            return;
        }
        getParentModel().renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.armorEntityGlint()),
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
    }
}
