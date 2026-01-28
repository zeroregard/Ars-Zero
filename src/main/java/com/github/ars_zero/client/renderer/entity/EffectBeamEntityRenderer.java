package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.client.renderer.ArsZeroRenderTypes;
import com.github.ars_zero.common.entity.EffectBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EffectBeamEntityRenderer extends EntityRenderer<EffectBeamEntity> {

    private static final double RAY_LENGTH = 300.0;
    private static final float BEAM_HALF_WIDTH = 0.05f;

    public EffectBeamEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EffectBeamEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        Vec3 origin = entity.position();
        Vec3 forward = entity.getForward();
        Vec3 end = origin.add(forward.scale(RAY_LENGTH));

        BlockHitResult blockHit = entity.level().clip(new ClipContext(origin, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
        EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                entity.level(), entity, origin, end, entity.getBoundingBox().inflate(RAY_LENGTH),
                e -> e.isPickable() && !e.isSpectator() && e != entity);

        Vec3 hitPos = end;
        if (entityHit != null) {
            double entityDist = origin.distanceTo(entityHit.getLocation());
            double blockDist = blockHit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : origin.distanceTo(blockHit.getLocation());
            if (entityDist < blockDist) {
                hitPos = entityHit.getLocation();
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                hitPos = blockHit.getLocation();
            }
        } else if (blockHit.getType() != HitResult.Type.MISS) {
            hitPos = blockHit.getLocation();
        }

        float dx = (float) (hitPos.x - entity.getX());
        float dy = (float) (hitPos.y - entity.getY());
        float dz = (float) (hitPos.z - entity.getZ());

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
            PoseStack.Pose pose = poseStack.last();
            VertexConsumer consumer = buffer.getBuffer(ArsZeroRenderTypes.positionColorTranslucentNoCull());
            float v0x = -rx - ux, v0y = -ry - uy, v0z = -rz - uz;
            float v1x =  rx - ux, v1y =  ry - uy, v1z =  rz - uz;
            float v2x =  rx + ux, v2y =  ry + uy, v2z =  rz + uz;
            float v3x = -rx + ux, v3y = -ry + uy, v3z = -rz + uz;
            float v4x = dx - rx - ux, v4y = dy - ry - uy, v4z = dz - rz - uz;
            float v5x = dx + rx - ux, v5y = dy + ry - uy, v5z = dz + rz - uz;
            float v6x = dx + rx + ux, v6y = dy + ry + uy, v6z = dz + rz + uz;
            float v7x = dx - rx + ux, v7y = dy - ry + uy, v7z = dz - rz + uz;
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
