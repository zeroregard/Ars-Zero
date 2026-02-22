package com.github.ars_zero.common.entity.ai;

import com.github.ars_zero.common.entity.MageSkeletonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * When the mage has a target and is within 20 blocks, take control of movement and stay put
 * (stop navigation) and face the target so head/torso rotate toward the player.
 */
public class MageSkeletonHoldPositionGoal extends Goal {

    private static final double STAY_WITHIN_BLOCKS = 20.0;

    private final MageSkeletonEntity mob;

    public MageSkeletonHoldPositionGoal(MageSkeletonEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return mob.distanceToSqr(target) <= STAY_WITHIN_BLOCKS * STAY_WITHIN_BLOCKS;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        mob.getNavigation().stop();
        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) {
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }
}
