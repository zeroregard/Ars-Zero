package com.github.ars_zero.common.gravity;

import com.github.ars_zero.common.attachment.GravitySuppressionAttachment;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.registry.ModAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class GravitySuppression {
    private GravitySuppression() {
    }

    public static void apply(Entity entity, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        GravitySuppressionAttachment attachment = entity.getData(ModAttachments.GRAVITY_SUPPRESSION);
        long expireTick = entity.level().getGameTime() + durationTicks;
        boolean useCustomSetter = entity instanceof BaseVoxelEntity;
        boolean originalNoGravity = useCustomSetter
            ? ((BaseVoxelEntity) entity).getNoGravityCustom()
            : entity.isNoGravity();
        GravitySuppressionAttachment updated = attachment.isActive()
            ? attachment.extend(useCustomSetter, expireTick)
            : attachment.activated(useCustomSetter, originalNoGravity, expireTick);
        entity.setData(ModAttachments.GRAVITY_SUPPRESSION, updated);
        enforce(entity, updated);
    }

    public static void tickPre(Entity entity) {
        GravitySuppressionAttachment attachment = entity.getData(ModAttachments.GRAVITY_SUPPRESSION);
        if (!attachment.isActive()) {
            return;
        }
        if (entity.level().isClientSide) {
            enforce(entity, attachment);
            return;
        }
        handleServerTick(entity, attachment);
    }

    public static void tickPost(Entity entity) {
        GravitySuppressionAttachment attachment = entity.getData(ModAttachments.GRAVITY_SUPPRESSION);
        if (!attachment.isActive()) {
            return;
        }
        if (entity.level().isClientSide) {
            enforce(entity, attachment);
            return;
        }
        handleServerTick(entity, attachment);
    }

    public static void forceRestore(Entity entity) {
        GravitySuppressionAttachment attachment = entity.getData(ModAttachments.GRAVITY_SUPPRESSION);
        if (!attachment.isActive()) {
            return;
        }
        restore(entity, attachment);
    }

    private static void handleServerTick(Entity entity, GravitySuppressionAttachment attachment) {
        if (entity.isRemoved()) {
            restore(entity, attachment);
            return;
        }
        long gameTime = entity.level().getGameTime();
        if (gameTime >= attachment.expireTick()) {
            restore(entity, attachment);
            return;
        }
        enforce(entity, attachment);
    }

    private static void enforce(Entity entity, GravitySuppressionAttachment attachment) {
        if (!attachment.isActive()) {
            return;
        }
        if (attachment.useCustomSetter() && entity instanceof BaseVoxelEntity voxel) {
            voxel.setNoGravityCustom(true);
        } else {
            entity.setNoGravity(true);
        }
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, 0.0, motion.z);
        entity.hasImpulse = true;
        entity.fallDistance = 0.0f;
    }

    private static void restore(Entity entity, GravitySuppressionAttachment attachment) {
        if (!attachment.isActive()) {
            return;
        }
        if (attachment.useCustomSetter() && entity instanceof BaseVoxelEntity voxel) {
            voxel.setNoGravityCustom(attachment.originalNoGravity());
        } else {
            entity.setNoGravity(attachment.originalNoGravity());
        }
        entity.fallDistance = 0.0f;
        entity.setData(ModAttachments.GRAVITY_SUPPRESSION, attachment.deactivate());
    }
}

