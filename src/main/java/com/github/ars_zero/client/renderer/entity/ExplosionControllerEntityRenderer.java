package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.ExplosionControllerEntity;
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

public class ExplosionControllerEntityRenderer extends GeoEntityRenderer<ExplosionControllerEntity> {
    
    private static final double RENDER_DISTANCE = 256.0;
    private static final int FULL_BRIGHTNESS = 15728880;
    private static final double ROTATION_SPEED_DEGREES_PER_SECOND = 45.0;
    
    public ExplosionControllerEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new ExplosionControllerEntityModel());
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }
    
    @Override
    public RenderType getRenderType(ExplosionControllerEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        if (animatable.isActive()) {
            return ArsZeroRenderTypes.eyesOpaqueNoCull(texture);
        }
        return ArsZeroRenderTypes.eyesNoCull(texture);
    }
    
    @Override
    public boolean shouldRender(ExplosionControllerEntity entity, Frustum frustum, double camX, double camY, double camZ) {
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
    public void render(ExplosionControllerEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isActive()) {
            int ticksSinceExplode = entity.tickCount - entity.getExplodeAnimationStartTick();
            if (ticksSinceExplode >= ExplosionControllerEntity.EXPLODE_ANIMATION_TICKS) {
                return;
            }
            
            float charge = entity.getCharge();
            float scale = charge;
            poseStack.scale(scale, scale, scale);
        }
        
        double totalTimeSeconds = (entity.tickCount + partialTick) / 20.0;
        float rotationAngle = (float) (totalTimeSeconds * ROTATION_SPEED_DEGREES_PER_SECOND);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationAngle));
        
        super.render(entity, entityYaw, partialTick, poseStack, buffer, FULL_BRIGHTNESS);
    }
    
    @Override
    public void actuallyRender(PoseStack poseStack, ExplosionControllerEntity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, FULL_BRIGHTNESS, packedOverlay, color);
    }
}

