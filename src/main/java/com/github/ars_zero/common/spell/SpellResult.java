package com.github.ars_zero.common.spell;

import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.IWrappedCaster;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.TileCaster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

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

    public final BlockGroupEntity blockGroup;
    public final List<BlockPos> blockPositions;

    public BlockPos userOffset;
    public int depth;

    public SpellResult(Entity targetEntity, BlockPos targetPosition, HitResult hitResult, SpellEffectType effectType,
            Vec3 relativeOffset, float casterYaw, float casterPitch, Vec3 casterPosition) {
        this(targetEntity, targetPosition, hitResult, effectType, relativeOffset, casterYaw, casterPitch,
                casterPosition, null, null);
    }

    public SpellResult(Entity targetEntity, BlockPos targetPosition, HitResult hitResult, SpellEffectType effectType,
            Vec3 relativeOffset, float casterYaw, float casterPitch, Vec3 casterPosition,
            BlockGroupEntity blockGroup, List<BlockPos> blockPositions) {
        this.targetEntity = targetEntity;
        this.targetPosition = targetPosition;
        this.hitResult = hitResult;
        this.effectType = effectType;
        this.timestamp = System.currentTimeMillis();
        this.relativeOffset = relativeOffset;
        this.casterYaw = casterYaw;
        this.casterPitch = casterPitch;
        this.casterPosition = casterPosition;
        this.blockGroup = blockGroup;
        this.blockPositions = blockPositions;
        this.userOffset = BlockPos.ZERO;
        this.depth = 1;
    }

    public static SpellResult fromHitResult(HitResult hitResult, SpellEffectType effectType) {
        return fromHitResultWithCaster(hitResult, effectType, null);
    }

    public static SpellResult fromHitResultWithCaster(HitResult hitResult, SpellEffectType effectType, IWrappedCaster caster) {
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
                CasterInfo casterInfo = extractCasterInfo(caster);
                casterPosition = casterInfo.position;
                casterYaw = casterInfo.yaw;
                casterPitch = casterInfo.pitch;

                relativeOffset = calculateRelativeOffsetInLocalSpace(
                        casterPosition, entityPos, casterYaw, casterPitch);
            }
        } else if (hitResult instanceof BlockHitResult blockHit) {
            blockPos = blockHit.getBlockPos();

            if (caster != null) {
                CasterInfo casterInfo = extractCasterInfo(caster);
                casterPosition = casterInfo.position;
                casterYaw = casterInfo.yaw;
                casterPitch = casterInfo.pitch;

                Vec3 blockCenter = Vec3.atCenterOf(blockPos);
                relativeOffset = calculateRelativeOffsetInLocalSpace(
                        casterPosition, blockCenter, casterYaw, casterPitch);
            }
        }

        return new SpellResult(entity, blockPos, hitResult, effectType,
                relativeOffset, casterYaw, casterPitch, casterPosition);
    }

    private static CasterInfo extractCasterInfo(IWrappedCaster caster) {
        Vec3 position;
        float yaw = 0;
        float pitch = 0;

        if (caster instanceof LivingCaster livingCaster && livingCaster.livingEntity != null) {
            LivingEntity living = livingCaster.livingEntity;
            position = living.getEyePosition(1.0f);
            yaw = living.getYRot();
            pitch = living.getXRot();
        } else if (caster instanceof TileCaster tileCaster) {
            position = tileCaster.getPosition().add(0.5, 0.5, 0.5);
            Direction facing = tileCaster.getFacingDirection();
            yaw = directionToYaw(facing);
            pitch = directionToPitch(facing);
        } else {
            position = caster.getPosition();
        }

        return new CasterInfo(position, yaw, pitch);
    }

    private static float directionToYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0f;
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            case EAST -> -90.0f;
            default -> 0.0f;
        };
    }

    private static float directionToPitch(Direction facing) {
        return switch (facing) {
            case UP -> -90.0f;
            case DOWN -> 90.0f;
            default -> 0.0f;
        };
    }

    private record CasterInfo(Vec3 position, float yaw, float pitch) {}

    public static SpellResult fromBlockGroup(BlockGroupEntity blockGroup, List<BlockPos> blockPositions,
            IWrappedCaster caster) {
        Vec3 relativeOffset = null;
        float casterYaw = 0;
        float casterPitch = 0;
        Vec3 casterPosition = null;

        if (caster != null) {
            CasterInfo casterInfo = extractCasterInfo(caster);
            casterPosition = casterInfo.position;
            casterYaw = casterInfo.yaw;
            casterPitch = casterInfo.pitch;

            Vec3 groupCenter = blockGroup.position();
            relativeOffset = calculateRelativeOffsetInLocalSpace(
                    casterPosition, groupCenter, casterYaw, casterPitch);
        }

        EntityHitResult fakeHit = new EntityHitResult(blockGroup);
        return new SpellResult(blockGroup, null, fakeHit, SpellEffectType.RESOLVED,
                relativeOffset, casterYaw, casterPitch, casterPosition,
                blockGroup, blockPositions);
    }

    /**
     * Creates a result that carries block positions only (no BlockGroupEntity).
     * Used when Select is augmented with Extract: blocks are propagated to later phases but not turned into a group.
     */
    public static SpellResult fromBlockPositions(List<BlockPos> blockPositions, IWrappedCaster caster) {
        if (blockPositions == null || blockPositions.isEmpty()) {
            throw new IllegalArgumentException("blockPositions must be non-empty");
        }
        BlockPos first = blockPositions.get(0);
        Vec3 center = Vec3.atCenterOf(first);
        if (blockPositions.size() > 1) {
            double x = 0, y = 0, z = 0;
            for (BlockPos pos : blockPositions) {
                x += pos.getX() + 0.5;
                y += pos.getY() + 0.5;
                z += pos.getZ() + 0.5;
            }
            int n = blockPositions.size();
            center = new Vec3(x / n, y / n, z / n);
        }
        Vec3 relativeOffset = null;
        float casterYaw = 0;
        float casterPitch = 0;
        Vec3 casterPosition = null;
        if (caster != null) {
            CasterInfo casterInfo = extractCasterInfo(caster);
            casterPosition = casterInfo.position;
            casterYaw = casterInfo.yaw;
            casterPitch = casterInfo.pitch;
            relativeOffset = calculateRelativeOffsetInLocalSpace(
                    casterPosition, center, casterYaw, casterPitch);
        }
        BlockHitResult blockHit = new BlockHitResult(center, Direction.UP, first, false);
        return new SpellResult(null, first, blockHit, SpellEffectType.RESOLVED,
                relativeOffset, casterYaw, casterPitch, casterPosition,
                null, blockPositions);
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
        return result;
    }

    public Vec3 transformLocalToWorld(float currentYaw, float currentPitch, Vec3 currentCasterPos) {
        return transformLocalToWorld(currentYaw, currentPitch, currentCasterPos, 1.0);
    }

    public Vec3 transformLocalToWorld(float currentYaw, float currentPitch, Vec3 currentCasterPos,
            double distanceMultiplier) {
        if (relativeOffset == null)
            return null;

        Vec3 scaledOffset = relativeOffset.scale(distanceMultiplier);

        double yawRad = Math.toRadians(currentYaw);
        double pitchRad = Math.toRadians(currentPitch);

        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);

        double rotatedZ = scaledOffset.z * cosPitch + scaledOffset.y * sinPitch;
        double rotatedY = scaledOffset.y * cosPitch - scaledOffset.z * sinPitch;

        double worldX = scaledOffset.x * cosYaw - rotatedZ * sinYaw;
        double worldZ = scaledOffset.x * sinYaw + rotatedZ * cosYaw;

        Vec3 worldOffset = new Vec3(worldX, rotatedY, worldZ);
        Vec3 result = currentCasterPos.add(worldOffset);
        return result;
    }
}
