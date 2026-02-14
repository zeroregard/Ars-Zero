package com.github.ars_zero.client.renderer.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Renders a staff as a simple 2D item texture by delegating to the vanilla item renderer.
 * Use for wands that have a flat texture (e.g. Wand of Telekinesis).
 */
public class Simple2DStaffRenderer extends BlockEntityWithoutLevelRenderer {

    public Simple2DStaffRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        Level level = Minecraft.getInstance().level;
        int seed = stack.isEmpty() ? 0 : stack.hashCode();
        itemRenderer.renderStatic(stack, displayContext, packedLight, packedOverlay, poseStack, bufferSource, level, seed);
    }
}
