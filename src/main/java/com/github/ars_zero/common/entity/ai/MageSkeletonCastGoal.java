package com.github.ars_zero.common.entity.ai;

import com.github.ars_zero.api.spell.MobSpellBehaviour;
import com.github.ars_zero.common.entity.AbstractBlightedSkeleton;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
    private static final float MIN_CAST_DISTANCE = 1.5f;
    private static final float MAX_CAST_DISTANCE = 22.0f;

    private final AbstractBlightedSkeleton mob;
    private final List<MobSpellBehaviour> behaviours;

    public MageSkeletonCastGoal(AbstractBlightedSkeleton mob, List<MobSpellBehaviour> behaviours) {
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
        var mana = CapabilityRegistry.getMana(mob);
        if (mana == null) {
            return;
        }
        double current = mana.getCurrentMana();
        List<MobSpellBehaviour> affordable = behaviours.stream()
                .filter(b -> current >= b.getManaCost() && b.canRun(mob, target))
                .toList();
        if (affordable.isEmpty()) {
            return;
        }
        MobSpellBehaviour behaviour = affordable.get(mob.getRandom().nextInt(affordable.size()));
        mob.swing(InteractionHand.MAIN_HAND);
        mob.setSpellCastArmsUpTicks(AbstractBlightedSkeleton.SPELL_CAST_ARMS_UP_TICKS);
        behaviour.run(mob, target);
        if (mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.8f, 0.9f + mob.getRandom().nextFloat() * 0.2f);
        }
    }
}
