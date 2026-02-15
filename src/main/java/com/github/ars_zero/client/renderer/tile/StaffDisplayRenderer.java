package com.github.ars_zero.client.renderer.tile;

import com.github.ars_zero.common.block.StaffDisplayBlockEntity;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.item.AbstractStaff;
import com.hollingsworth.arsnouveau.client.ClientInfo;
import com.hollingsworth.arsnouveau.common.block.ArcanePedestal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class StaffDisplayRenderer implements BlockEntityRenderer<StaffDisplayBlockEntity> {

    private static final float MODEL_TO_BLOCK_SCALE = 1f / 16f;

    public StaffDisplayRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static Vector3f getDisplayTranslation(ItemStack stack) {
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
        var transforms = model.getTransforms();
        var thirdPerson = transforms.getTransform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        return new Vector3f(thirdPerson.translation);
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

        Vector3f displayTranslation = getDisplayTranslation(tile.getStack());
        float tx = displayTranslation.x * MODEL_TO_BLOCK_SCALE;
        float ty = displayTranslation.y * MODEL_TO_BLOCK_SCALE + 0.75f;
        if (tile.getStack().getItem() instanceof AbstractSpellStaff) {
            ty += 0.25f;
        }
        float tz = displayTranslation.z * MODEL_TO_BLOCK_SCALE;

        poseStack.pushPose();
        poseStack.translate(xOffset + tx, yOffset + ty, zOffset + tz);

        if (tile.getStack().getItem() instanceof AbstractStaff) {
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
