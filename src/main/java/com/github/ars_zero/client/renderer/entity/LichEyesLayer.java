package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.renderer.entity.model.BlightedSkeletonModel;
import com.github.ars_zero.common.entity.LichBlightedSkeleton;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.world.entity.monster.Skeleton;

/**
 * Renders glowing eyes on the Lich by drawing an eyes overlay texture at full brightness.
 * Only active when the entity is a {@link LichBlightedSkeleton}.
 */
public class LichEyesLayer extends EyesLayer<Skeleton, BlightedSkeletonModel> {

    private static final RenderType LICH_EYES =
            RenderType.eyes(ArsZero.prefix("textures/entity/skeleton/lich_eyes.png"));

    public LichEyesLayer(RenderLayerParent<Skeleton, BlightedSkeletonModel> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return LICH_EYES;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       Skeleton entity, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(entity instanceof LichBlightedSkeleton)) {
            return;
        }
        super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount,
                partialTick, ageInTicks, netHeadYaw, headPitch);
    }
}
