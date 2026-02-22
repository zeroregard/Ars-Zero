package com.github.ars_zero.common.entity.ai;

import com.github.ars_zero.api.spell.MobSpellBehaviour;
import com.github.ars_zero.common.entity.MageSkeletonEntity;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that runs a spell behaviour (e.g. blight voxel + Push) at the current target when mana and cooldown allow.
 * Blaze-style cooldown to avoid spamming. Designed for future chance-based spell selection.
 */
public class MageSkeletonCastGoal extends Goal {

    public static final int COOLDOWN_TICKS = 30;
    private static final float MIN_CAST_DISTANCE = 4.0f;
    private static final float MAX_CAST_DISTANCE = 22.0f;

    private final MageSkeletonEntity mob;
    private final List<MobSpellBehaviour> behaviours;

    public MageSkeletonCastGoal(MageSkeletonEntity mob, List<MobSpellBehaviour> behaviours) {
        this.mob = mob;
        this.behaviours = List.copyOf(behaviours);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public boolean canUse() {
        if (mob.getCastCooldownTicks() > 0 || mob.getChargeTicks() > 0) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distSq = mob.distanceToSqr(target);
        if (distSq < MIN_CAST_DISTANCE * MIN_CAST_DISTANCE || distSq > MAX_CAST_DISTANCE * MAX_CAST_DISTANCE) {
            return false;
        }
        var mana = CapabilityRegistry.getMana(mob);
        if (mana == null) {
            return false;
        }
        double current = mana.getCurrentMana();
        for (MobSpellBehaviour b : behaviours) {
            if (current >= b.getManaCost()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        for (MobSpellBehaviour behaviour : behaviours) {
            var mana = CapabilityRegistry.getMana(mob);
            if (mana != null && mana.getCurrentMana() >= behaviour.getManaCost() && behaviour.canRun(mob, target)) {
                behaviour.run(mob, target);
                return;
            }
        }
    }
}
