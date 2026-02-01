package com.github.ars_zero.client.event;

import com.github.ars_zero.common.entity.EffectBeamEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import com.github.ars_zero.common.util.MathHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "ars_zero", value = Dist.CLIENT)
public class EffectBeamClientSyncHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        mc.level.entitiesForRendering().forEach(entity -> {
            if (entity instanceof EffectBeamEntity beam && beam.getCasterUUID() != null
                    && beam.getCasterUUID().equals(mc.player.getUUID())) {
                updateBeamRotationToLookPoint(beam, mc.player);
            }
        });
    }

    private static void updateBeamRotationToLookPoint(EffectBeamEntity beam, net.minecraft.world.entity.LivingEntity caster) {
        Vec3 eyePos = caster.getEyePosition(1.0f);
        Vec3 lookVec = caster.getLookAngle();
        Vec3 end = eyePos.add(lookVec.scale(EffectBeamEntity.RAY_LENGTH));
        BlockHitResult blockHit = beam.level().clip(new ClipContext(eyePos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(caster, eyePos, end, caster.getBoundingBox().inflate(EffectBeamEntity.RAY_LENGTH), e -> e.isPickable() && !e.isSpectator() && e != beam && e != caster && !(e instanceof EffectBeamEntity), EffectBeamEntity.RAY_LENGTH * EffectBeamEntity.RAY_LENGTH);
        Vec3 targetPoint;
        if (entityHit != null) {
            double entityDist = eyePos.distanceTo(entityHit.getLocation());
            double blockDist = blockHit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : eyePos.distanceTo(blockHit.getLocation());
            targetPoint = entityDist < blockDist ? entityHit.getLocation() : (blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation());
        } else {
            targetPoint = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        }
        Vec3 toTarget = targetPoint.subtract(beam.position());
        if (toTarget.lengthSqr() < 1.0E-12) {
            return;
        }
        float[] yawPitch = MathHelper.vecToYawPitch(toTarget.normalize());
        beam.setYRot(yawPitch[0]);
        beam.setXRot(yawPitch[1]);
    }
}
