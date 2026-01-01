package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.explosion.ExplosionControllerEntity;
import com.github.ars_zero.client.renderer.ArsZeroRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ExplosionControllerEntityRenderer extends GeoEntityRenderer<ExplosionControllerEntity> {

    private static final double RENDER_DISTANCE = 256.0;
    private static final int FULL_BRIGHTNESS = 15728880;
    private static final double ROTATION_SPEED_DEGREES_PER_SECOND = 45.0;
    private static final double POSITION_OFFSET_MIN = -0.5;
    private static final double POSITION_OFFSET_MAX = 0.5;
    private static final double POSITION_OFFSET_Y = 0.5;
    private static final double LERP_SPEED = 0.15;
    private static final int TARGET_UPDATE_INTERVAL = 10;

    private static class PositionOffset {
        double targetX;
        double targetZ;
        double currentX;
        double currentZ;
        int lastUpdateTick;

        PositionOffset(double targetX, double targetZ) {
            this.targetX = targetX;
            this.targetZ = targetZ;
            this.currentX = targetX;
            this.currentZ = targetZ;
            this.lastUpdateTick = 0;
        }
    }

    private final Map<Integer, PositionOffset> positionOffsets = new HashMap<>();

    public ExplosionControllerEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new ExplosionControllerEntityModel());
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }

    @Override
    public RenderType getRenderType(ExplosionControllerEntity animatable, ResourceLocation texture,
            MultiBufferSource bufferSource, float partialTick) {
        if (animatable.isExploding()) {
            return ArsZeroRenderTypes.eyesOpaqueNoCull(texture);
        }
        return ArsZeroRenderTypes.eyesNoCull(texture);
    }

    @Override
    public boolean shouldRender(ExplosionControllerEntity entity, Frustum frustum, double camX, double camY,
            double camZ) {
        if (entity.isExploding()) {
            int ticksSinceExplode = entity.tickCount - entity.getExplodeAnimationStartTick();
            int durationTicks = entity.getExplodeAnimationDurationTicks();
            if (ticksSinceExplode >= durationTicks) {
                return false;
            }
        }

        Vec3 entityPos = entity.position();
        double dx = entityPos.x - camX;
        double dy = entityPos.y - camY;
        double dz = entityPos.z - camZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        double maxDistanceSq = RENDER_DISTANCE * RENDER_DISTANCE;
        if (distanceSq > maxDistanceSq) {
            return false;
        }

        AABB boundingBox = new AABB(entityPos.x - 1.0, entityPos.y - 1.0, entityPos.z - 1.0,
                entityPos.x + 1.0, entityPos.y + 1.0, entityPos.z + 1.0);
        return frustum.isVisible(boundingBox);
    }

    @Override
    public void render(ExplosionControllerEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        int entityId = entity.getId();
        PositionOffset offset = positionOffsets.computeIfAbsent(entityId, id -> {
            Random random = new Random(id);
            double targetX = POSITION_OFFSET_MIN + random.nextDouble() * (POSITION_OFFSET_MAX - POSITION_OFFSET_MIN);
            double targetZ = POSITION_OFFSET_MIN + random.nextDouble() * (POSITION_OFFSET_MAX - POSITION_OFFSET_MIN);
            return new PositionOffset(targetX, targetZ);
        });

        int currentTick = entity.tickCount;
        if (currentTick - offset.lastUpdateTick >= TARGET_UPDATE_INTERVAL) {
            Random random = new Random(entityId + currentTick);
            offset.targetX = POSITION_OFFSET_MIN + random.nextDouble() * (POSITION_OFFSET_MAX - POSITION_OFFSET_MIN);
            offset.targetZ = POSITION_OFFSET_MIN + random.nextDouble() * (POSITION_OFFSET_MAX - POSITION_OFFSET_MIN);
            offset.lastUpdateTick = currentTick;
        }

        double lerpAmount = LERP_SPEED * (1.0 + partialTick);
        offset.currentX += (offset.targetX - offset.currentX) * lerpAmount;
        offset.currentZ += (offset.targetZ - offset.currentZ) * lerpAmount;

        poseStack.translate(offset.currentX, POSITION_OFFSET_Y, offset.currentZ);

        int remainingLifespan = entity.getLifespan();

        if (!entity.isExploding() && remainingLifespan < 10) {
            float lifespanScale = Math.max(0.0f, (float) remainingLifespan / 10.0f);
            poseStack.scale(lifespanScale, lifespanScale, lifespanScale);
        }

        if (entity.isExploding()) {
            int ticksSinceExplode = entity.tickCount - entity.getExplodeAnimationStartTick();
            int durationTicks = entity.getExplodeAnimationDurationTicks();
            if (ticksSinceExplode >= durationTicks) {
                return;
            }

            float charge = entity.getCharge();
            double radius = entity.getRadius();
            float scale = charge + ((float) radius / 14f);
            poseStack.scale(scale, scale, scale);
        }

        int maxLifespan = entity.getMaxLifespan();
        double lifespanSpeedMultiplier = calculateLifespanSpeedMultiplier(remainingLifespan, maxLifespan);

        double totalTimeSeconds = (entity.tickCount + partialTick) / 20.0;
        double rotationSpeed = ROTATION_SPEED_DEGREES_PER_SECOND * lifespanSpeedMultiplier;
        float rotationAngle = (float) (totalTimeSeconds * rotationSpeed);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationAngle));

        super.render(entity, entityYaw, partialTick, poseStack, buffer, FULL_BRIGHTNESS);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, ExplosionControllerEntity animatable, BakedGeoModel model,
            RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, int color) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick,
                FULL_BRIGHTNESS, packedOverlay, color);
    }

    private double calculateLifespanSpeedMultiplier(int remainingLifespan, int maxLifespan) {
        if (maxLifespan <= 0) {
            return 1.0;
        }

        if (remainingLifespan <= 1) {
            return maxLifespan * 10.0;
        }

        double normalizedLifespan = (double) remainingLifespan / maxLifespan;
        double inverseNormalized = 1.0 / Math.max(0.01, normalizedLifespan);

        return Math.max(1.0, inverseNormalized);
    }
}
