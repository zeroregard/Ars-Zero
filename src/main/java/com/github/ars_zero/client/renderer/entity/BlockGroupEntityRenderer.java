package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockGroupEntityRenderer extends EntityRenderer<BlockGroupEntity> {
    
    private final BlockRenderDispatcher blockRenderer;
    
    public BlockGroupEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
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
        
        Vec3 entityPos = entity.position();
        
        poseStack.pushPose();
        
        for (var blockData : blocks) {
            BlockState blockState = blockData.blockState;
            
            if (blockState.getRenderShape() == RenderShape.INVISIBLE) {
                continue;
            }
            
            Vec3 blockWorldPos = entityPos.add(blockData.relativePosition).subtract(0.5, 0.5, 0.5);
            
            poseStack.pushPose();
            poseStack.translate(blockData.relativePosition.x - 0.5, blockData.relativePosition.y - 0.5, blockData.relativePosition.z - 0.5);
            
            BlockPos renderPos = BlockPos.containing(blockWorldPos);
            int combinedLight = LevelRenderer.getLightColor(entity.level(), blockState, renderPos);
            
            try {
                this.blockRenderer.renderSingleBlock(
                    blockState,
                    poseStack,
                    buffer,
                    combinedLight,
                    OverlayTexture.NO_OVERLAY
                );
            } catch (Exception e) {
                ArsZero.LOGGER.warn("Failed to render block {} for BlockGroupEntity", blockState, e);
            }
            
            poseStack.popPose();
        }
        
        poseStack.popPose();
    }
    
    @Override
    public ResourceLocation getTextureLocation(BlockGroupEntity entity) {
        return null;
    }
}

