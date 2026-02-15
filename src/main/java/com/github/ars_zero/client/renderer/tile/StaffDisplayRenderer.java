package com.github.ars_zero.client.renderer.tile;

import com.github.ars_zero.common.block.StaffDisplayBlockEntity;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.hollingsworth.arsnouveau.client.ClientInfo;
import com.hollingsworth.arsnouveau.common.block.ArcanePedestal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class StaffDisplayRenderer implements BlockEntityRenderer<StaffDisplayBlockEntity> {

    public StaffDisplayRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(StaffDisplayBlockEntity tile, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (tile.getStack().isEmpty()) {
            return;
        }
        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof ArcanePedestal pedestal)) {
            return;
        }
        Vector3f offsetVec = pedestal.getItemOffset(state, tile.getBlockPos());
        float xOffset = offsetVec.x - tile.getBlockPos().getX();
        float yOffset = offsetVec.y - tile.getBlockPos().getY();
        float zOffset = offsetVec.z - tile.getBlockPos().getZ();

        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset, zOffset);
        poseStack.scale(0.5f, 0.5f, 0.5f);

        if (tile.getStack().getItem() instanceof AbstractSpellStaff) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            Minecraft.getInstance().getItemRenderer().renderStatic(tile.getStack(),
                    ItemDisplayContext.GROUND,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    bufferSource,
                    tile.getLevel(),
                    (int) tile.getBlockPos().asLong());
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees((partialTick + (float) ClientInfo.ticksInGame) * 3f));
            Minecraft.getInstance().getItemRenderer().renderStatic(tile.getStack(),
                    ItemDisplayContext.FIXED,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    bufferSource,
                    tile.getLevel(),
                    (int) tile.getBlockPos().asLong());
        }

        poseStack.popPose();
    }
}
