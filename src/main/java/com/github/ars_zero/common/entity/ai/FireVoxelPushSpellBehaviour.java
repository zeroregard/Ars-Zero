package com.github.ars_zero.common.entity.ai;

import com.github.ars_zero.api.spell.MobSpellBehaviour;
import com.github.ars_zero.common.casting.CastingStyle;
import com.github.ars_zero.common.entity.ArcaneCircleEntity;
import com.github.ars_zero.common.entity.AbstractBlightedSkeleton;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.LichBlightedSkeleton;
import com.github.ars_zero.common.util.MathHelper;
import com.github.ars_zero.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Spell behaviour: fire casting circle + fire voxel hover for 2s, then Push toward target.
 */
public class FireVoxelPushSpellBehaviour implements MobSpellBehaviour {

    /** How long the circle and voxel hover before being pushed (2 seconds). */
    public static final int HOVER_TICKS = BlightVoxelPushSpellBehaviour.HOVER_TICKS;
    public static final int CIRCLE_LIFESPAN_TICKS = HOVER_TICKS;
    public static final int VOXEL_LIFETIME_TICKS = BlightVoxelPushSpellBehaviour.BLIGHT_VOXEL_LIFETIME_TICKS;
    private static final int MANA_COST = 30;

    @Override
    public int getManaCost() {
        return MANA_COST;
    }

    @Override
    public boolean run(Mob caster, LivingEntity target) {
        if (!(caster.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(caster.getEyePosition(1.0f)).normalize();
        double horizontalLength = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        float yaw = (float) (Math.atan2(-toTarget.x, toTarget.z) * 180.0 / Math.PI);
        float pitch = (float) (Math.atan2(-toTarget.y, horizontalLength) * 180.0 / Math.PI);
        caster.setYRot(yaw);
        caster.setXRot(pitch);

        CastingStyle style = new CastingStyle();
        style.setEnabled(true);
        style.setPlacement(CastingStyle.Placement.NEAR);
        style.setColor(0xFF6A00);
        style.setActiveBones(java.util.Set.of());

        Vec3 spawnPos = caster.getEyePosition(1.0f).add(caster.getLookAngle().scale(1.0));

        ArcaneCircleEntity circleEntity = ModEntities.ARCANE_CIRCLE.get().create(serverLevel);
        if (circleEntity != null) {
            circleEntity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            circleEntity.setSyncedRotation(yaw, pitch);
            circleEntity.initialize(caster, style);
            circleEntity.setCurrentSchoolId("school_fire");
            circleEntity.setMaxAliveTicks(CIRCLE_LIFESPAN_TICKS);
            serverLevel.addFreshEntity(circleEntity);
        }

        boolean isLich = caster instanceof LichBlightedSkeleton;
        int voxelCount = isLich ? 3 : 1;
        java.util.List<Vec3> voxelPositions = voxelCount == 1
                ? java.util.List.of(spawnPos)
                : MathHelper.getCirclePositions(spawnPos, caster.getLookAngle(), 0.35, voxelCount);
        java.util.List<Integer> voxelIds = new java.util.ArrayList<>();

        for (Vec3 pos : voxelPositions) {
            FireVoxelEntity voxel = new FireVoxelEntity(serverLevel, pos.x, pos.y, pos.z, VOXEL_LIFETIME_TICKS);
            voxel.setNoGravityCustom(true);
            serverLevel.addFreshEntity(voxel);
            voxelIds.add(voxel.getId());
        }

        if (caster instanceof AbstractBlightedSkeleton mage) {
            if (voxelIds.size() == 1) {
                mage.setPendingPush(voxelIds.get(0), HOVER_TICKS);
            } else {
                mage.setPendingPushMultiple(voxelIds, HOVER_TICKS);
            }
            return true;
        }
        return false;
    }
}
