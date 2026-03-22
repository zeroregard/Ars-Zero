package com.github.ars_zero.client.renderer.item;

import com.github.ars_zero.common.block.tile.BoneChestBlockEntity;
import com.github.ars_zero.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BoneChestItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final BlockEntityRenderDispatcher dispatcher;
    private BoneChestBlockEntity fakeEntity;

    public BoneChestItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
        this.dispatcher = dispatcher;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack,
                             MultiBufferSource buffer, int light, int overlay) {
        if (fakeEntity == null) {
            fakeEntity = new BoneChestBlockEntity(BlockPos.ZERO, ModBlocks.BONE_CHEST.get().defaultBlockState());
        }
        dispatcher.renderItem(fakeEntity, poseStack, buffer, light, overlay);
    }
}
