package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.terrain.ConjureTerrainConvergenceEntity;
import com.github.ars_zero.common.structure.ConvergenceStructureHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.List;

public class ConjureTerrainConvergenceEntityRenderer extends GeoEntityRenderer<ConjureTerrainConvergenceEntity> {
    private static final float BUILDING_RED = 0.2f;
    private static final float BUILDING_GREEN = 0.9f;
    private static final float BUILDING_BLUE = 0.35f;

    private static final float WAITING_RED = 0.9f;
    private static final float WAITING_GREEN = 0.2f;
    private static final float WAITING_BLUE = 0.2f;

    private static final float PAUSED_RED = 0.9f;
    private static final float PAUSED_GREEN = 0.9f;
    private static final float PAUSED_BLUE = 0.2f;

    private static final double RENDER_DISTANCE = 128.0;

    public ConjureTerrainConvergenceEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new ConjureTerrainConvergenceEntityModel());
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }

    @Override
    public RenderType getRenderType(ConjureTerrainConvergenceEntity animatable, ResourceLocation texture,
            MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void render(ConjureTerrainConvergenceEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {

        if (entity.isBuilding()) {
            poseStack.pushPose();
            poseStack.translate(0, 0.5, 0);

            float targetYaw = calculateYawToTarget(entity);
            float smoothedYaw = entity.getSmoothedYaw(targetYaw);
            poseStack.mulPose(Axis.YP.rotationDegrees(-smoothedYaw));

            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            poseStack.popPose();
            return;
        }

        if (entity.getLifespan() <= 0) {
            return;
        }

        Player clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer != null) {
            java.util.UUID casterUuid = entity.getCasterUUID();
            if (casterUuid != null && casterUuid.equals(clientPlayer.getUUID())) {
                renderPreviewOutline(entity, poseStack, buffer);
            }
        }
    }

    private float calculateYawToTarget(ConjureTerrainConvergenceEntity entity) {
        BlockPos target = entity.getTargetBlock();
        if (target == null) {
            return 0f;
        }

        Vec3 entityPos = entity.position();
        double dx = (target.getX() + 0.5) - entityPos.x;
        double dz = (target.getZ() + 0.5) - entityPos.z;

        return (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
    }

    private void renderPreviewOutline(ConjureTerrainConvergenceEntity entity, PoseStack poseStack,
            MultiBufferSource buffer) {
        Vec3 entityPos = entity.position();
        BlockPos centerBlock = BlockPos.containing(entityPos);

        List<BlockPos> blockPositions = ConvergenceStructureHelper.generate(centerBlock, entity.getSize(),
                ConvergenceStructureHelper.Shape.CUBE);

        float r, g, b;
        if (entity.isPaused()) {
            r = PAUSED_RED;
            g = PAUSED_GREEN;
            b = PAUSED_BLUE;
        } else if (entity.isWaitingForMana()) {
            r = WAITING_RED;
            g = WAITING_GREEN;
            b = WAITING_BLUE;
        } else {
            r = BUILDING_RED;
            g = BUILDING_GREEN;
            b = BUILDING_BLUE;
        }

        poseStack.pushPose();

        int size = Math.max(1, entity.getSize());
        if (size == 1) {
            double blockX = centerBlock.getX() - entityPos.x;
            double blockY = centerBlock.getY() - entityPos.y;
            double blockZ = centerBlock.getZ() - entityPos.z;
            renderBlockBox(poseStack, buffer, blockX, blockY, blockZ, r, g, b);
            poseStack.popPose();
            return;
        }

        VertexConsumer lines = buffer.getBuffer(RenderType.lines());

        int minOffset = entity.getMinOffset();
        int maxOffset = entity.getMaxOffset();

        BlockPos minBlock = centerBlock.offset(minOffset, minOffset, minOffset);
        BlockPos maxBlock = centerBlock.offset(maxOffset, maxOffset, maxOffset);

        double minX = minBlock.getX() - entityPos.x;
        double minY = minBlock.getY() - entityPos.y;
        double minZ = minBlock.getZ() - entityPos.z;
        double maxX = maxBlock.getX() + 1.0 - entityPos.x;
        double maxY = maxBlock.getY() + 1.0 - entityPos.y;
        double maxZ = maxBlock.getZ() + 1.0 - entityPos.z;

        LevelRenderer.renderLineBox(poseStack, lines, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, 1.0f);

        for (BlockPos blockPos : blockPositions) {
            double blockX = blockPos.getX() - entityPos.x;
            double blockY = blockPos.getY() - entityPos.y;
            double blockZ = blockPos.getZ() - entityPos.z;

            int relativeX = blockPos.getX() - centerBlock.getX();
            int relativeY = blockPos.getY() - centerBlock.getY();
            int relativeZ = blockPos.getZ() - centerBlock.getZ();

            if (!ConvergenceStructureHelper.isSurface(relativeX, relativeY, relativeZ, minOffset, maxOffset,
                    ConvergenceStructureHelper.Shape.CUBE)) {
                continue;
            }
            renderBlockBox(poseStack, buffer, blockX, blockY, blockZ, r, g, b);
        }

        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(ConjureTerrainConvergenceEntity entity, Frustum frustum, double camX, double camY,
            double camZ) {
        Vec3 entityPos = entity.position();
        double dx = entityPos.x - camX;
        double dy = entityPos.y - camY;
        double dz = entityPos.z - camZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        double maxDistanceSq = RENDER_DISTANCE * RENDER_DISTANCE;
        if (distanceSq > maxDistanceSq) {
            return false;
        }

        if (entity.isBuilding()) {
            AABB box = new AABB(entityPos.x - 2.0, entityPos.y - 2.0, entityPos.z - 2.0, entityPos.x + 2.0,
                    entityPos.y + 2.0, entityPos.z + 2.0);
            return frustum.isVisible(box);
        }

        int size = Math.max(1, entity.getSize());
        double pad = (size / 2.0) + 1.0;
        AABB box = new AABB(entityPos.x - pad, entityPos.y - pad, entityPos.z - pad, entityPos.x + pad,
                entityPos.y + pad, entityPos.z + pad);
        return frustum.isVisible(box);
    }

    private void renderBlockBox(PoseStack poseStack, MultiBufferSource buffer, double x, double y, double z,
            float r, float g, float b) {
        VertexConsumer lines = buffer.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(
                poseStack,
                lines,
                x,
                y,
                z,
                x + 1.0,
                y + 1.0,
                z + 1.0,
                r,
                g,
                b,
                1.0f);
    }
}
