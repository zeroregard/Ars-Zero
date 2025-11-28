package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class BlockGroupEntityRenderer extends EntityRenderer<BlockGroupEntity> {
    private static final float OUTLINE_RED = 0.2f;
    private static final float OUTLINE_GREEN = 0.8f;
    private static final float OUTLINE_BLUE = 1.0f;
    private final BlockRenderDispatcher blockRenderer;
    
    public BlockGroupEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }
    
    @Override
    public void render(BlockGroupEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isEmpty()) {
            return;
        }
        
        var blocks = entity.getBlocks();
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        
        poseStack.pushPose();
        VertexConsumer outlineConsumer = buffer.getBuffer(RenderType.lines());
        
        for (var blockData : blocks) {
            BlockState blockState = blockData.blockState;
            
            if (blockState.getRenderShape() == RenderShape.INVISIBLE) {
                continue;
            }
            
            poseStack.pushPose();
            poseStack.translate(blockData.relativePosition.x - 0.5, blockData.relativePosition.y - 0.5, blockData.relativePosition.z - 0.5);
            
            try {
                this.blockRenderer.renderSingleBlock(
                    blockState,
                    poseStack,
                    buffer,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY
                );
            } catch (Exception e) {
                ArsZero.LOGGER.warn("Failed to render block {} for BlockGroupEntity", blockState, e);
            }
            
            renderOutline(poseStack, outlineConsumer);
            
            poseStack.popPose();
        }
        
        poseStack.popPose();
    }
    
    @Override
    public ResourceLocation getTextureLocation(BlockGroupEntity entity) {
        return null;
    }
    
    private void renderOutline(PoseStack poseStack, VertexConsumer outlineConsumer) {
        LevelRenderer.renderLineBox(
            poseStack,
            outlineConsumer,
            0.0,
            0.0,
            0.0,
            1.0,
            1.0,
            1.0,
            OUTLINE_RED,
            OUTLINE_GREEN,
            OUTLINE_BLUE,
            1.0f
        );
    }
}

