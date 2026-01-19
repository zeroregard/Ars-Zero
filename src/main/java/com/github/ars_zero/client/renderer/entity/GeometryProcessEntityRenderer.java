package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.IGeometryProcessEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Common renderer for geometry process entities (terrain/break).
 * Renders the same golem when building and the preview outline when idle.
 */
public class GeometryProcessEntityRenderer<T extends Entity & IGeometryProcessEntity & GeoAnimatable> extends GeoEntityRenderer<T> {

    protected record Palette(float clearR, float clearG, float clearB,
                             float adjacentR, float adjacentG, float adjacentB,
                             float blockedR, float blockedG, float blockedB) {}

    private final Palette palette;

    public GeometryProcessEntityRenderer(EntityRendererProvider.Context context, GeoModel<T> model, Palette palette) {
        super(context, model);
        this.palette = palette;
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight) {
        Player clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer == null) {
            return;
        }

        UUID casterUuid = entity.getCasterUUID();
        if (casterUuid == null || !casterUuid.equals(clientPlayer.getUUID())) {
            return;
        }

        if (entity.isProcessing()) {
            poseStack.pushPose();
            poseStack.translate(0, 0.5, 0);
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            poseStack.popPose();
            return;
        }

        Vec3 entityRenderPos = entity.getPosition(partialTicks);
        BlockPos entityBlockPos = entity.blockPosition();
        Vec3 entityBlockCenter = Vec3.atCenterOf(entityBlockPos);
        Vec3 interpolationOffset = entityRenderPos.subtract(entityBlockCenter);

        poseStack.pushPose();
        poseStack.translate(-interpolationOffset.x, -interpolationOffset.y, -interpolationOffset.z);
        renderPreviewOutline(entity, poseStack, buffer);
        poseStack.popPose();
    }

    private void renderPreviewOutline(T entity, PoseStack poseStack, MultiBufferSource buffer) {
        BlockPos entityBlockPos = entity.blockPosition();
        Vec3 entityPos = Vec3.atCenterOf(entityBlockPos);

        List<BlockPos> positions = entity.generatePositions(entityBlockPos);
        Map<BlockPos, IGeometryProcessEntity.BlockStatus> statuses = entity.getBlockStatuses();

        for (BlockPos pos : positions) {
            IGeometryProcessEntity.BlockStatus status = statuses.getOrDefault(pos, IGeometryProcessEntity.BlockStatus.CLEAR);

            float red;
            float green;
            float blue;
            switch (status) {
                case CLEAR -> {
                    red = palette.clearR();
                    green = palette.clearG();
                    blue = palette.clearB();
                }
                case ADJACENT -> {
                    red = palette.adjacentR();
                    green = palette.adjacentG();
                    blue = palette.adjacentB();
                }
                case BLOCKED -> {
                    red = palette.blockedR();
                    green = palette.blockedG();
                    blue = palette.blockedB();
                }
                default -> {
                    red = palette.clearR();
                    green = palette.clearG();
                    blue = palette.clearB();
                }
            }

            double blockX = pos.getX() - entityPos.x;
            double blockY = pos.getY() - entityPos.y;
            double blockZ = pos.getZ() - entityPos.z;

            VertexConsumer lines = buffer.getBuffer(RenderType.lines());
            LevelRenderer.renderLineBox(
                    poseStack, lines,
                    blockX, blockY, blockZ,
                    blockX + 1.0, blockY + 1.0, blockZ + 1.0,
                    red, green, blue, 0.7f
            );
        }
    }
}

