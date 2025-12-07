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
import net.minecraft.world.phys.Vec3;

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
        
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;
        
        for (var blockData : blocks) {
            BlockState blockState = blockData.blockState;
            
            if (blockState.getRenderShape() == RenderShape.INVISIBLE) {
                continue;
            }
            
            Vec3 pos = blockData.relativePosition;
            minX = Math.min(minX, pos.x - 0.5);
            minY = Math.min(minY, pos.y - 0.5);
            minZ = Math.min(minZ, pos.z - 0.5);
            maxX = Math.max(maxX, pos.x + 0.5);
            maxY = Math.max(maxY, pos.y + 0.5);
            maxZ = Math.max(maxZ, pos.z + 0.5);
            
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
            
            poseStack.popPose();
        }
        
        if (minX != Double.MAX_VALUE) {
            VertexConsumer outlineConsumer = buffer.getBuffer(RenderType.lines());
            renderGroupOutline(poseStack, outlineConsumer, minX, minY, minZ, maxX, maxY, maxZ);
        }
        
        poseStack.popPose();
    }
    
    @Override
    public ResourceLocation getTextureLocation(BlockGroupEntity entity) {
        return null;
    }
    
    private void renderGroupOutline(PoseStack poseStack, VertexConsumer outlineConsumer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        LevelRenderer.renderLineBox(
            poseStack,
            outlineConsumer,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            OUTLINE_RED,
            OUTLINE_GREEN,
            OUTLINE_BLUE,
            1.0f
        );
    }
}

