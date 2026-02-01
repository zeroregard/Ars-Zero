package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.renderer.ArsZeroRenderTypes;
import com.github.ars_zero.common.entity.EffectBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Random;

public class EffectBeamEntityRenderer extends EntityRenderer<EffectBeamEntity> {

    private static final float BEAM_HALF_WIDTH = 0.05f;
    private static final float SHAKE_RANGE = 0.03f;
    private static final float ORIGIN_CIRCLE_SIZE = 0.15f;
    private static final ResourceLocation BEAM_ORIGIN_CIRCLE_TEXTURE = ArsZero.prefix("textures/entity/beam_origin_circle.png");

    private static final double RENDER_DISTANCE = 256.0;

    public EffectBeamEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }

    @Override
    public boolean shouldRender(EffectBeamEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        Vec3 origin = entity.position();
        Vec3 end = entity.getEffectiveEndPoint(origin);
        double minX = Math.min(origin.x, end.x) - 0.5;
        double minY = Math.min(origin.y, end.y) - 0.5;
        double minZ = Math.min(origin.z, end.z) - 0.5;
        double maxX = Math.max(origin.x, end.x) + 0.5;
        double maxY = Math.max(origin.y, end.y) + 0.5;
        double maxZ = Math.max(origin.z, end.z) + 0.5;
        double dx = (minX + maxX) / 2.0 - camX;
        double dy = (minY + maxY) / 2.0 - camY;
        double dz = (minZ + maxZ) / 2.0 - camZ;
        if (dx * dx + dy * dy + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) {
            return false;
        }
        return frustum.isVisible(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    @Override
    public void render(EffectBeamEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        Vec3 origin = entity.position();
        Vec3 hitPos = entity.getEffectiveEndPoint(origin);

        float dx = (float) (hitPos.x - entity.getX());
        float dy = (float) (hitPos.y - entity.getY());
        float dz = (float) (hitPos.z - entity.getZ());

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(ORIGIN_CIRCLE_SIZE, ORIGIN_CIRCLE_SIZE, ORIGIN_CIRCLE_SIZE);
        PoseStack.Pose originPose = poseStack.last();
        VertexConsumer originConsumer = buffer.getBuffer(ArsZeroRenderTypes.entityTranslucentEmissiveFullBright(BEAM_ORIGIN_CIRCLE_TEXTURE));
        float cr = entity.getColorR();
        float cg = entity.getColorG();
        float cb = entity.getColorB();
        float ca = 0.9f;
        originConsumer.addVertex(originPose.pose(), -1, -1, 0).setUv(0, 0).setColor(cr, cg, cb, ca).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(originPose, 0f, 0f, 1f);
        originConsumer.addVertex(originPose.pose(), -1, 1, 0).setUv(0, 1).setColor(cr, cg, cb, ca).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(originPose, 0f, 0f, 1f);
        originConsumer.addVertex(originPose.pose(), 1, 1, 0).setUv(1, 1).setColor(cr, cg, cb, ca).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(originPose, 0f, 0f, 1f);
        originConsumer.addVertex(originPose.pose(), 1, -1, 0).setUv(1, 0).setColor(cr, cg, cb, ca).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(originPose, 0f, 0f, 1f);
        poseStack.popPose();

        Vec3 dir = new Vec3(dx, dy, dz);
        double len = dir.length();
        if (len > 1.0E-6) {
            Vec3 fwd = dir.normalize();
            Vec3 right;
            if (Math.abs(fwd.y) < 0.99) {
                right = fwd.cross(new Vec3(0, 1, 0)).normalize();
            } else {
                right = fwd.cross(new Vec3(0, 0, 1)).normalize();
            }
            Vec3 up = fwd.cross(right).normalize();
            float rx = (float) (right.x * BEAM_HALF_WIDTH);
            float ry = (float) (right.y * BEAM_HALF_WIDTH);
            float rz = (float) (right.z * BEAM_HALF_WIDTH);
            float ux = (float) (up.x * BEAM_HALF_WIDTH);
            float uy = (float) (up.y * BEAM_HALF_WIDTH);
            float uz = (float) (up.z * BEAM_HALF_WIDTH);
            float r = entity.getColorR();
            float g = entity.getColorG();
            float b = entity.getColorB();
            float alpha = 0.9f;
            Random rand = new Random(entity.tickCount * 31L);
            float s0x = (rand.nextFloat() - 0.5f) * 2.0f * SHAKE_RANGE;
            float s0y = (rand.nextFloat() - 0.5f) * 2.0f * SHAKE_RANGE;
            float s0z = (rand.nextFloat() - 0.5f) * 2.0f * SHAKE_RANGE;
            float s1x = (rand.nextFloat() - 0.5f) * 2.0f * SHAKE_RANGE;
            float s1y = (rand.nextFloat() - 0.5f) * 2.0f * SHAKE_RANGE;
            float s1z = (rand.nextFloat() - 0.5f) * 2.0f * SHAKE_RANGE;
            PoseStack.Pose pose = poseStack.last();
            VertexConsumer consumer = buffer.getBuffer(ArsZeroRenderTypes.positionColorTranslucentNoCull());
            float v0x = s0x - rx - ux, v0y = s0y - ry - uy, v0z = s0z - rz - uz;
            float v1x = s0x + rx - ux, v1y = s0y + ry - uy, v1z = s0z + rz - uz;
            float v2x = s0x + rx + ux, v2y = s0y + ry + uy, v2z = s0z + rz + uz;
            float v3x = s0x - rx + ux, v3y = s0y - ry + uy, v3z = s0z - rz + uz;
            float v4x = dx + s1x - rx - ux, v4y = dy + s1y - ry - uy, v4z = dz + s1z - rz - uz;
            float v5x = dx + s1x + rx - ux, v5y = dy + s1y + ry - uy, v5z = dz + s1z + rz - uz;
            float v6x = dx + s1x + rx + ux, v6y = dy + s1y + ry + uy, v6z = dz + s1z + rz + uz;
            float v7x = dx + s1x - rx + ux, v7y = dy + s1y - ry + uy, v7z = dz + s1z - rz + uz;
            consumer.addVertex(pose, v0x, v0y, v0z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v1x, v1y, v1z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v2x, v2y, v2z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v3x, v3y, v3z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v4x, v4y, v4z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v7x, v7y, v7z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v6x, v6y, v6z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v5x, v5y, v5z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v1x, v1y, v1z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v5x, v5y, v5z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v6x, v6y, v6z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v2x, v2y, v2z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v0x, v0y, v0z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v3x, v3y, v3z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v7x, v7y, v7z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v4x, v4y, v4z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v3x, v3y, v3z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v2x, v2y, v2z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v6x, v6y, v6z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v7x, v7y, v7z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v0x, v0y, v0z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v4x, v4y, v4z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v5x, v5y, v5z).setColor(r, g, b, alpha);
            consumer.addVertex(pose, v1x, v1y, v1z).setColor(r, g, b, alpha);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EffectBeamEntity entity) {
        return null;
    }
}
