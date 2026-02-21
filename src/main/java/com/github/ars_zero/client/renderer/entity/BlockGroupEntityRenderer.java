package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.HashMap;
import java.util.Map;

public class BlockGroupEntityRenderer extends EntityRenderer<BlockGroupEntity> {
    /** Travel delta at which we consider motion "full" for transparency (blocks per tick). */
    private static final float TRAVEL_DELTA_SCALE = 2f;
    /** Max transparency when moving at full scale (1 - this = min alpha). */
    private static final float MAX_TRANSPARENCY = 0.55f;
    /** Lerp toward synced travel delta per frame (rise). */
    private static final float SMOOTH_RISE = 0.35f;
    /** Lerp toward 0 per frame (decay back to opaque). */
    private static final float SMOOTH_DECAY = 0.06f;

    private final BlockRenderDispatcher blockRenderer;
    /** Client-only: smoothed travel delta per entity for gradual transparency effect. */
    private final Map<Integer, Float> smoothedTravelDelta = new HashMap<>();

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

        float smoothed = updateSmoothedTravelDelta(entity, partialTicks);
        float intensity = Math.min(1f, smoothed / TRAVEL_DELTA_SCALE);
        float motionAlpha = 1f - intensity * MAX_TRANSPARENCY;
        // Do not set shader color here: cutout/alpha-test (leaves) would discard with alpha < 1.

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;
        
        BlockAndTintGetter level = entity.level() instanceof BlockAndTintGetter getter ? getter : null;

        // One RandomSource per entity so glint phase is identical for all blocks in the group.
        RandomSource random = RandomSource.create(42L);
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
                renderBlock(blockState, blockData, entity, level, poseStack, buffer, random, packedLight);
                if (level != null) {
                    renderBlockGlint(blockState, level, poseStack, buffer, random);
                }
            } catch (Exception e) {
                ArsZero.LOGGER.warn("Failed to render block {} for BlockGroupEntity", blockState, e);
            }
            poseStack.popPose();
        }

        if (minX != Double.MAX_VALUE) {
            int packed = entity.getOutlineColor();
            float r = ((packed >> 16) & 0xFF) / 255f;
            float g = ((packed >> 8) & 0xFF) / 255f;
            float b = (packed & 0xFF) / 255f;
            VertexConsumer outlineConsumer = buffer.getBuffer(RenderType.lines());
            float prevLineWidth = RenderSystem.getShaderLineWidth();
            RenderSystem.lineWidth(prevLineWidth * 2.0f);
            renderGroupOutline(poseStack, outlineConsumer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, motionAlpha);
            RenderSystem.lineWidth(prevLineWidth);
        }

        poseStack.popPose();
    }
    
    @Override
    public ResourceLocation getTextureLocation(BlockGroupEntity entity) {
        return null;
    }

    /**
     * Updates client-side smoothed travel delta: lerps toward synced value (rise) and toward 0 (decay)
     * so the motion effect is gradual, not boolean. Returns the smoothed value for this frame.
     */
    private float updateSmoothedTravelDelta(BlockGroupEntity entity, float partialTicks) {
        int id = entity.getId();
        float current = smoothedTravelDelta.getOrDefault(id, 0f);
        float synced = entity.getTravelDelta();
        float rise = SMOOTH_RISE * partialTicks;
        float decay = SMOOTH_DECAY * partialTicks;
        current = current + (synced - current) * rise;
        current = current + (0f - current) * decay;
        smoothedTravelDelta.put(id, current);
        return current;
    }

    /**
     * Renders a block so leaves (cutout/cutoutMipped) and other layers work in the entity pass.
     * When level is present we use the block model's render types, map to entity buffers via
     * RenderTypeHelper.getEntityRenderType, and renderBatched with level/worldPos for biome tint.
     * Otherwise we fall back to renderSingleBlock.
     */
    private void renderBlock(BlockState blockState, BlockGroupEntity.BlockData blockData, BlockGroupEntity entity,
                            BlockAndTintGetter level, PoseStack poseStack, MultiBufferSource buffer, RandomSource random,
                            int packedLight) {
        BlockPos worldPos = BlockPos.containing(
            entity.getX() + blockData.relativePosition.x,
            entity.getY() + blockData.relativePosition.y,
            entity.getZ() + blockData.relativePosition.z
        );
        if (level != null) {
            var model = blockRenderer.getBlockModel(blockState);
            for (RenderType chunkType : model.getRenderTypes(blockState, random, ModelData.EMPTY)) {
                RenderType entityType = RenderTypeHelper.getEntityRenderType(chunkType, false);
                VertexConsumer consumer = buffer.getBuffer(entityType);
                blockRenderer.renderBatched(blockState, worldPos, level, poseStack, consumer, false, random, ModelData.EMPTY, chunkType);
            }
        } else {
            int light = LightTexture.FULL_BRIGHT;
            this.blockRenderer.renderSingleBlock(blockState, poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        }
    }

    private void renderBlockGlint(BlockState blockState, BlockAndTintGetter level, PoseStack poseStack, MultiBufferSource buffer, RandomSource random) {
        if (level == null) return;
        RenderType glintType = RenderType.entityGlintDirect();
        VertexConsumer glintConsumer = buffer.getBuffer(glintType);
        blockRenderer.renderBatched(blockState, BlockPos.ZERO, level, poseStack, glintConsumer, false, random, ModelData.EMPTY, glintType);
    }

    private void renderGroupOutline(PoseStack poseStack, VertexConsumer outlineConsumer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float r, float g, float b, float alpha) {
        LevelRenderer.renderLineBox(poseStack, outlineConsumer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, alpha);
    }
}

