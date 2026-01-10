package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.terrain.ConjureTerrainConvergenceEntity;
import com.github.ars_zero.common.structure.ConvergenceStructureHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ConjureTerrainConvergenceEntityRenderer extends EntityRenderer<ConjureTerrainConvergenceEntity> {
    private static final float RED = 0.2f;
    private static final float GREEN = 0.9f;
    private static final float BLUE = 0.35f;
    private static final float MARKER_RED = 0.95f;
    private static final float MARKER_GREEN = 0.65f;
    private static final float MARKER_BLUE = 0.15f;
    private static final double RENDER_DISTANCE = 128.0;

    public ConjureTerrainConvergenceEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }

    @Override
    public void render(ConjureTerrainConvergenceEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        if (entity.isBuilding()) {
            renderMarker(poseStack, buffer, entity.isPaused());
            return;
        }
        if (entity.getLifespan() <= 0) {
            return;
        }

        int size = Math.max(1, entity.getSize());
        if (size == 1) {
            renderBlockBox(poseStack, buffer, 0, 0, 0);
            return;
        }

        VertexConsumer lines = buffer.getBuffer(RenderType.lines());

        int minOffset = entity.getMinOffset();
        int maxOffset = entity.getMaxOffset();

        double min = minOffset - 0.5;
        double max = maxOffset + 0.5;
        LevelRenderer.renderLineBox(poseStack, lines, min, min, min, max, max, max, RED, GREEN, BLUE, 1.0f);

        for (int dy = minOffset; dy <= maxOffset; dy++) {
            for (int dx = minOffset; dx <= maxOffset; dx++) {
                for (int dz = minOffset; dz <= maxOffset; dz++) {
                    if (!ConvergenceStructureHelper.isSurface(dx, dy, dz, minOffset, maxOffset,
                            ConvergenceStructureHelper.Shape.CUBE)) {
                        continue;
                    }
                    renderBlockBox(poseStack, buffer, dx, dy, dz);
                }
            }
        }
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

    @Override
    public ResourceLocation getTextureLocation(ConjureTerrainConvergenceEntity entity) {
        return null;
    }

    private void renderBlockBox(PoseStack poseStack, MultiBufferSource buffer, int dx, int dy, int dz) {
        VertexConsumer lines = buffer.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(
                poseStack,
                lines,
                dx - 0.5,
                dy - 0.5,
                dz - 0.5,
                dx + 0.5,
                dy + 0.5,
                dz + 0.5,
                RED,
                GREEN,
                BLUE,
                1.0f
        );
    }

    private void renderMarker(PoseStack poseStack, MultiBufferSource buffer, boolean paused) {
        VertexConsumer lines = buffer.getBuffer(RenderType.lines());
        float r = paused ? 1.0f : MARKER_RED;
        float g = paused ? 0.2f : MARKER_GREEN;
        float b = paused ? 0.2f : MARKER_BLUE;
        LevelRenderer.renderLineBox(poseStack, lines, -0.35, -0.35, -0.35, 0.35, 0.35, 0.35, r, g, b, 1.0f);
        LevelRenderer.renderLineBox(poseStack, lines, -0.10, -0.55, -0.10, 0.10, 0.55, 0.10, r, g, b, 1.0f);
        LevelRenderer.renderLineBox(poseStack, lines, -0.10, -0.10, -0.55, 0.10, 0.10, 0.55, r, g, b, 1.0f);
        LevelRenderer.renderLineBox(poseStack, lines, -0.55, -0.10, -0.10, 0.55, 0.10, 0.10, r, g, b, 1.0f);
    }
}

