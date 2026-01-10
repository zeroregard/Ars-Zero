package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.terrain.ConjureTerrainConvergenceEntity;
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
    private static final double RENDER_DISTANCE = 128.0;

    public ConjureTerrainConvergenceEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }

    @Override
    public void render(ConjureTerrainConvergenceEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        if (entity.isBuilding() || entity.getLifespan() <= 0) {
            return;
        }

        int half = entity.getHalfExtent();
        if (half <= 0) {
            renderBlockBox(poseStack, buffer, 0, 0, 0);
            return;
        }

        VertexConsumer lines = buffer.getBuffer(RenderType.lines());

        double min = -half - 0.5;
        double max = half + 0.5;
        LevelRenderer.renderLineBox(poseStack, lines, min, min, min, max, max, max, RED, GREEN, BLUE, 1.0f);

        for (int dy = -half; dy <= half; dy++) {
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    if (!isSurface(dx, dy, dz, half)) {
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

        int half = Math.max(0, entity.getHalfExtent());
        double pad = half + 1.0;
        AABB box = new AABB(entityPos.x - pad, entityPos.y - pad, entityPos.z - pad, entityPos.x + pad,
                entityPos.y + pad, entityPos.z + pad);
        return frustum.isVisible(box);
    }

    @Override
    public ResourceLocation getTextureLocation(ConjureTerrainConvergenceEntity entity) {
        return null;
    }

    private static boolean isSurface(int dx, int dy, int dz, int half) {
        return Math.abs(dx) == half || Math.abs(dy) == half || Math.abs(dz) == half;
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
}

