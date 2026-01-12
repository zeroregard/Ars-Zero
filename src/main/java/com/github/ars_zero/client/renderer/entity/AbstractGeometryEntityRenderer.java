package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.AbstractGeometryProcessEntity;
import com.github.ars_zero.common.entity.IGeometryProcessEntity.BlockStatus;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Map;
import java.util.UUID;

/**
 * Shared renderer for geometry-processing entities (Terrain, Break, etc.)
 */
public class AbstractGeometryEntityRenderer<T extends AbstractGeometryProcessEntity & GeoEntity>
        extends GeoEntityRenderer<T> {

    protected static final float CLEAR_RED = 0.2f;
    protected static final float CLEAR_GREEN = 0.9f;
    protected static final float CLEAR_BLUE = 0.35f;

    protected static final float ADJACENT_RED = 0.9f;
    protected static final float ADJACENT_GREEN = 0.9f;
    protected static final float ADJACENT_BLUE = 0.2f;

    protected static final float BLOCKED_RED = 0.9f;
    protected static final float BLOCKED_GREEN = 0.2f;
    protected static final float BLOCKED_BLUE = 0.2f;

    protected static final double RENDER_DISTANCE = 128.0;

    public AbstractGeometryEntityRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture,
            MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack,
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
            UUID casterUuid = entity.getCasterUUID();
            if (casterUuid != null && casterUuid.equals(clientPlayer.getUUID())) {
                renderPreviewOutline(entity, poseStack, buffer, partialTicks);
            }
        }
    }

    protected float calculateYawToTarget(T entity) {
        BlockPos target = entity.getTargetBlock();
        if (target == null) {
            return 0f;
        }

        Vec3 entityPos = entity.position();
        double dx = (target.getX() + 0.5) - entityPos.x;
        double dz = (target.getZ() + 0.5) - entityPos.z;

        return (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
    }

    protected void renderPreviewOutline(T entity, PoseStack poseStack,
            MultiBufferSource buffer, float partialTicks) {
        // Get the effective center (entity position + user offset) for block generation
        BlockPos effectiveCenter = entity.getEffectiveCenter();
        Vec3 effectiveCenterVec = Vec3.atCenterOf(effectiveCenter);
        
        // Entity render position (where the entity visually is)
        Vec3 entityRenderPos = entity.getPosition(partialTicks);
        
        // Offset from entity visual position to effective center
        Vec3 offsetToEffective = effectiveCenterVec.subtract(entityRenderPos);

        Map<BlockPos, BlockStatus> blockStatuses = entity.getBlockStatuses();

        poseStack.pushPose();
        // Translate to render blocks at their world positions, offset from entity render position
        poseStack.translate(offsetToEffective.x, offsetToEffective.y, offsetToEffective.z);

        for (Map.Entry<BlockPos, BlockStatus> entry : blockStatuses.entrySet()) {
            BlockPos blockPos = entry.getKey();
            BlockStatus status = entry.getValue();

            float r, g, b;
            switch (status) {
                case BLOCKED -> {
                    r = BLOCKED_RED;
                    g = BLOCKED_GREEN;
                    b = BLOCKED_BLUE;
                }
                case ADJACENT -> {
                    r = ADJACENT_RED;
                    g = ADJACENT_GREEN;
                    b = ADJACENT_BLUE;
                }
                default -> {
                    r = CLEAR_RED;
                    g = CLEAR_GREEN;
                    b = CLEAR_BLUE;
                }
            }

            double blockX = blockPos.getX() - effectiveCenterVec.x;
            double blockY = blockPos.getY() - effectiveCenterVec.y;
            double blockZ = blockPos.getZ() - effectiveCenterVec.z;
            renderBlockBox(poseStack, buffer, blockX, blockY, blockZ, r, g, b);
        }

        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double camX, double camY, double camZ) {
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

    protected void renderBlockBox(PoseStack poseStack, MultiBufferSource buffer, double x, double y, double z,
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

