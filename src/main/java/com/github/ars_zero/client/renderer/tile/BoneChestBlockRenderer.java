package com.github.ars_zero.client.renderer.tile;

import com.github.ars_zero.client.renderer.model.BoneChestGeoModel;
import com.github.ars_zero.common.block.tile.BoneChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class BoneChestBlockRenderer extends GeoBlockRenderer<BoneChestBlockEntity> {

    public BoneChestBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new BoneChestGeoModel());
    }

    @Override
    public void preRender(PoseStack poseStack, BoneChestBlockEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int color) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, color);

        poseStack.translate(0.375, 0.0, -0.375);
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
    }
}
