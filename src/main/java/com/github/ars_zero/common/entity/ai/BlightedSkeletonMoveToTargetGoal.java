package com.github.ars_zero.common.entity.ai;

import com.github.ars_zero.common.entity.AbstractBlightedSkeleton;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Path toward the current target when they are beyond hold/cast range.
 * Without this, blighted skeletons would never close in on the player.
 */
public class BlightedSkeletonMoveToTargetGoal extends Goal {

    /** Chase until within this distance; matches HoldPositionGoal's boundary. */
    private static final double MOVE_TO_WITHIN_BLOCKS = 20.0;
    private static final double MOVE_SPEED = 1.0;

    private final AbstractBlightedSkeleton mob;

    public BlightedSkeletonMoveToTargetGoal(AbstractBlightedSkeleton mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return mob.distanceToSqr(target) > MOVE_TO_WITHIN_BLOCKS * MOVE_TO_WITHIN_BLOCKS;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return mob.distanceToSqr(target) > MOVE_TO_WITHIN_BLOCKS * MOVE_TO_WITHIN_BLOCKS
                && mob.getNavigation().isInProgress();
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target != null) {
            mob.getNavigation().moveTo(target, MOVE_SPEED);
        }
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (mob.distanceToSqr(target) > MOVE_TO_WITHIN_BLOCKS * MOVE_TO_WITHIN_BLOCKS
                && !mob.getNavigation().isInProgress()) {
            mob.getNavigation().moveTo(target, MOVE_SPEED);
        }
    }
}
