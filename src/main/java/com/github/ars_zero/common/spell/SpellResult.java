package com.github.ars_zero.common.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SpellResult {
    public final Entity targetEntity;
    public final BlockPos targetPosition;
    public final HitResult hitResult;
    public final SpellEffectType effectType;
    public final long timestamp;
    
    public final Vec3 relativeOffset;
    public final float casterYaw;
    public final float casterPitch;
    public final Vec3 casterPosition;
    
    public SpellResult(Entity targetEntity, BlockPos targetPosition, HitResult hitResult, SpellEffectType effectType,
                      Vec3 relativeOffset, float casterYaw, float casterPitch, Vec3 casterPosition) {
        this.targetEntity = targetEntity;
        this.targetPosition = targetPosition;
        this.hitResult = hitResult;
        this.effectType = effectType;
        this.timestamp = System.currentTimeMillis();
        this.relativeOffset = relativeOffset;
        this.casterYaw = casterYaw;
        this.casterPitch = casterPitch;
        this.casterPosition = casterPosition;
    }
    
    public static SpellResult fromHitResult(HitResult hitResult, SpellEffectType effectType) {
        return fromHitResultWithCaster(hitResult, effectType, null);
    }
    
    public static SpellResult fromHitResultWithCaster(HitResult hitResult, SpellEffectType effectType, Player caster) {
        Entity entity = null;
        BlockPos blockPos = null;
        Vec3 relativeOffset = null;
        float casterYaw = 0;
        float casterPitch = 0;
        Vec3 casterPosition = null;
        
        if (hitResult instanceof EntityHitResult entityHit) {
            entity = entityHit.getEntity();
            
            if (caster != null && entity != null) {
                Vec3 entityPos = entity.position();
                casterPosition = caster.getEyePosition(1.0f);
                casterYaw = caster.getYRot();
                casterPitch = caster.getXRot();
                
                relativeOffset = calculateRelativeOffsetInLocalSpace(
                    casterPosition, entityPos, casterYaw, casterPitch
                );
            }
        } else if (hitResult instanceof BlockHitResult blockHit) {
            blockPos = blockHit.getBlockPos();
            
            if (caster != null) {
                casterPosition = caster.getEyePosition(1.0f);
                casterYaw = caster.getYRot();
                casterPitch = caster.getXRot();
                
                Vec3 blockCenter = Vec3.atCenterOf(blockPos);
                relativeOffset = calculateRelativeOffsetInLocalSpace(
                    casterPosition, blockCenter, casterYaw, casterPitch
                );
            }
        }
        
        return new SpellResult(entity, blockPos, hitResult, effectType, 
                             relativeOffset, casterYaw, casterPitch, casterPosition);
    }
    
    private static Vec3 calculateRelativeOffsetInLocalSpace(Vec3 casterPos, Vec3 targetPos, float yaw, float pitch) {
        Vec3 worldOffset = targetPos.subtract(casterPos);
        
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        
        double rotatedX = worldOffset.x * cosYaw + worldOffset.z * sinYaw;
        double rotatedZ = -worldOffset.x * sinYaw + worldOffset.z * cosYaw;
        
        double localX = rotatedX;
        double localY = rotatedZ * sinPitch + worldOffset.y * cosPitch;
        double localZ = rotatedZ * cosPitch - worldOffset.y * sinPitch;
        
        Vec3 result = new Vec3(localX, localY, localZ);
        com.github.ars_zero.ArsZero.LOGGER.debug("Captured: world={}, yaw={}, pitch={} -> local={}", worldOffset, yaw, pitch, result);
        return result;
    }
    
    public Vec3 transformLocalToWorld(float currentYaw, float currentPitch, Vec3 currentCasterPos) {
        if (relativeOffset == null) return null;
        
        double yawRad = Math.toRadians(currentYaw);
        double pitchRad = Math.toRadians(currentPitch);
        
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        
        double rotatedZ = relativeOffset.z * cosPitch + relativeOffset.y * sinPitch;
        double rotatedY = relativeOffset.y * cosPitch - relativeOffset.z * sinPitch;
        
        double worldX = relativeOffset.x * cosYaw - rotatedZ * sinYaw;
        double worldZ = relativeOffset.x * sinYaw + rotatedZ * cosYaw;
        
        Vec3 worldOffset = new Vec3(worldX, rotatedY, worldZ);
        Vec3 result = currentCasterPos.add(worldOffset);
        com.github.ars_zero.ArsZero.LOGGER.debug("Transform: local={}, yaw={}, pitch={} -> world offset={}, final={}", relativeOffset, currentYaw, currentPitch, worldOffset, result);
        return result;
    }
}
