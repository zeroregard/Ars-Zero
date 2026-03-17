package com.github.ars_zero.client.renderer.tile;

import com.github.ars_zero.client.renderer.model.BoneChestGeoModel;
import com.github.ars_zero.common.block.BoneChestBlock;
import com.github.ars_zero.common.block.tile.BoneChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
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

        BlockState state = animatable.getBlockState();
        // When rendered as an item (BEWLR), the fake entity has no level — default to WEST.
        Direction facing = (animatable.getLevel() != null && state.hasProperty(BoneChestBlock.FACING))
                ? state.getValue(BoneChestBlock.FACING)
                : Direction.WEST;

        // Translation is computed per-facing so the model's visual centre lands at (0.5, y, 0.5).
        // Derived from the confirmed-working WEST case: translate(0.375, 0, -0.375) + rotate(180°).
        // Model geo centre in local space = (-0.125, y, -0.875). Each case shifts that to (0.5, y, 0.5).
        switch (facing) {
            case EAST  -> { poseStack.translate( 0.625, 0.0,  1.375); poseStack.mulPose(Axis.YP.rotationDegrees(  0)); }
            case SOUTH -> { poseStack.translate(-0.375, 0.0,  0.625); poseStack.mulPose(Axis.YP.rotationDegrees( 90)); }
            case WEST  -> { poseStack.translate( 0.375, 0.0, -0.375); poseStack.mulPose(Axis.YP.rotationDegrees(180)); }
            case NORTH -> { poseStack.translate( 1.375, 0.0,  0.375); poseStack.mulPose(Axis.YP.rotationDegrees(270)); }
            default    -> { poseStack.translate( 0.375, 0.0, -0.375); poseStack.mulPose(Axis.YP.rotationDegrees(180)); }
        }
    }
}
