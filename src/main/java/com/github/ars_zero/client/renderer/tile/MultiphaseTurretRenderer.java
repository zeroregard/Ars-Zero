package com.github.ars_zero.client.renderer.tile;

import com.github.ars_zero.common.block.MultiphaseSpellTurretTile;
import com.hollingsworth.arsnouveau.client.renderer.tile.ArsGeoBlockRenderer;
import com.hollingsworth.arsnouveau.common.block.BasicSpellTurret;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;

public class MultiphaseTurretRenderer extends ArsGeoBlockRenderer<MultiphaseSpellTurretTile> {
    
    private static final GeoModel<MultiphaseSpellTurretTile> MODEL = new MultiphaseTurretModel();

    public MultiphaseTurretRenderer(BlockEntityRendererProvider.Context rendererDispatcherIn) {
        super(rendererDispatcherIn, MODEL);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, MultiphaseSpellTurretTile animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        poseStack.pushPose();
        Direction direction = animatable.getBlockState().getValue(BasicSpellTurret.FACING);
        if (direction == Direction.UP) {
            poseStack.translate(0, 0.5, -0.5);
        } else if (direction == Direction.DOWN) {
            poseStack.translate(0, 0.5, 0.5);
        }
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
        poseStack.popPose();
    }
}



