package com.github.ars_zero.common.entity.ai;

import com.github.ars_zero.common.entity.AbstractBlightedSkeleton;
import com.github.ars_zero.common.entity.BoneGolem;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * When the lich has a target and fewer than getMaxSummons() bone golems nearby, spawns one BoneGolem.
 */
public class LichSummonGoal extends Goal {

    private static final int MANA_COST = 150;
    private static final int SUMMON_COOLDOWN_TICKS = 60;
    private static final int GOLEM_SEARCH_RADIUS = 32;

    private final AbstractBlightedSkeleton mob;

    public LichSummonGoal(AbstractBlightedSkeleton mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.getCastCooldownTicks() > 0 || mob.getChargeTicks() > 0 || mob.getSummonCooldownTicks() > 0) {
            return false;
        }
        if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
            return false;
        }
        if (countNearbyGolems() >= mob.getMaxSummons()) {
            return false;
        }
        var mana = CapabilityRegistry.getMana(mob);
        return mana != null && mana.getCurrentMana() >= MANA_COST;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        Level level = mob.level();
        if (!(level instanceof ServerLevel serverLevel)) return;
        var mana = CapabilityRegistry.getMana(mob);
        if (mana == null || mana.getCurrentMana() < MANA_COST) return;

        mob.swing(InteractionHand.MAIN_HAND);
        mob.setSpellCastArmsUpTicks(AbstractBlightedSkeleton.SPELL_CAST_ARMS_UP_TICKS);

        BoneGolem golem = new BoneGolem(ModEntities.BONE_GOLEM.get(), serverLevel);

        int dx = mob.getRandom().nextInt(5) - 2;
        int dz = mob.getRandom().nextInt(5) - 2;
        golem.moveTo(mob.getX() + dx, mob.getY(), mob.getZ() + dz, mob.getRandom().nextFloat() * 360f, 0f);
        golem.finalizeSpawn((ServerLevelAccessor) serverLevel, serverLevel.getCurrentDifficultyAt(golem.blockPosition()), MobSpawnType.MOB_SUMMONED, null);

        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) {
            golem.setTarget(target);
        }

        serverLevel.addFreshEntity(golem);
        serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 0.7f, 0.9f + mob.getRandom().nextFloat() * 0.2f);

        mana.setMana(mana.getCurrentMana() - MANA_COST);
        mob.setCastCooldownTicks(MageSkeletonCastGoal.COOLDOWN_TICKS);
        mob.setSummonCooldownTicks(SUMMON_COOLDOWN_TICKS);
    }

    private int countNearbyGolems() {
        AABB box = mob.getBoundingBox().inflate(GOLEM_SEARCH_RADIUS);
        List<BoneGolem> golems = mob.level().getEntitiesOfClass(BoneGolem.class, box);
        return golems.size();
    }
}
