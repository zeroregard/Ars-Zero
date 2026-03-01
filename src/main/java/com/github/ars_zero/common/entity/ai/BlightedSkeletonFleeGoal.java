package com.github.ars_zero.common.entity.ai;

import com.github.ars_zero.common.entity.AbstractBlightedSkeleton;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * When the blighted skeleton has a target but mana is below threshold, path away from the target.
 * Only registered for tiers that return true from shouldFleeWhenLowMana() (e.g. Acolyte).
 */
public class BlightedSkeletonFleeGoal extends Goal {

    private static final double FLEE_MANA_THRESHOLD = 50.0;
    private static final double FLEE_SPEED = 1.2;
    private static final double FLEE_MIN_DISTANCE = 8.0;
    private static final int HORIZONTAL_RANGE = 10;

    private final AbstractBlightedSkeleton mob;

    public BlightedSkeletonFleeGoal(AbstractBlightedSkeleton mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!mob.shouldFleeWhenLowMana()) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        var mana = CapabilityRegistry.getMana(mob);
        if (mana == null || mana.getCurrentMana() >= FLEE_MANA_THRESHOLD) {
            return false;
        }
        return mob.distanceToSqr(target) < FLEE_MIN_DISTANCE * FLEE_MIN_DISTANCE;
    }

    @Override
    public boolean canContinueToUse() {
        if (!canUse()) return false;
        var mana = CapabilityRegistry.getMana(mob);
        return mana != null && mana.getCurrentMana() < FLEE_MANA_THRESHOLD;
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        Vec3 away = mob.position().subtract(target.position()).normalize();
        Vec3 randomPos = DefaultRandomPos.getPosAway(mob, HORIZONTAL_RANGE, 8, away);
        if (randomPos != null) {
            mob.getNavigation().moveTo(randomPos.x, randomPos.y, randomPos.z, FLEE_SPEED);
        }
    }
}
