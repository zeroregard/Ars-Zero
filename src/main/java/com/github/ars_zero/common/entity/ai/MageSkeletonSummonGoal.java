package com.github.ars_zero.common.entity.ai;

import com.github.ars_zero.common.entity.AbstractBlightedSkeleton;
import com.github.ars_zero.registry.ModMobEffects;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import alexthw.ars_elemental.common.entity.summon.SummonUndead;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.EnumSet;

/**
 * When the mage has a target but no living revenant, spawns one SummonUndead (Ars-Elemental).
 * At most one revenant at a time: the mage tracks the summoned entity by UUID and will not
 * summon again until that entity is dead or missing.
 * Runs at priority 1 so he summons before casting blight when he doesn't have a skeleton.
 */
public class MageSkeletonSummonGoal extends Goal {

    private static final int MANA_COST = 150;
    private static final int SUMMON_DURATION_TICKS = 15 * 20;
    /** Ticks to block summoning after a spawn so the new entity is registered before we can consider summoning again. */
    private static final int SUMMON_COOLDOWN_TICKS = 60;

    private final AbstractBlightedSkeleton mob;

    public MageSkeletonSummonGoal(AbstractBlightedSkeleton mob) {
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
        if (mob.countLivingOwnedRevenants() >= mob.getMaxSummons()) {
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
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        var mana = CapabilityRegistry.getMana(mob);
        if (mana == null || mana.getCurrentMana() < MANA_COST) {
            return;
        }

        mob.swing(InteractionHand.MAIN_HAND);
        mob.setSpellCastArmsUpTicks(com.github.ars_zero.common.entity.AbstractBlightedSkeleton.SPELL_CAST_ARMS_UP_TICKS);
        SummonUndead summon = new SummonUndead(serverLevel);
        summon.setWeapon(new ItemStack(Items.IRON_SWORD));
        summon.setOwner(mob);
        summon.setLimitedLife(SUMMON_DURATION_TICKS);
        LivingEntity mageTarget = mob.getTarget();
        if (mageTarget != null && mageTarget.isAlive()) {
            summon.setTarget(mageTarget);
        }

        int dx = mob.getRandom().nextInt(5) - 2;
        int dz = mob.getRandom().nextInt(5) - 2;
        double x = mob.getX() + dx;
        double y = mob.getY();
        double z = mob.getZ() + dz;
        summon.moveTo(x, y, z, 0.0F, 0.0F);
        summon.finalizeSpawn((ServerLevelAccessor) serverLevel, serverLevel.getCurrentDifficultyAt(summon.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        summon.addEffect(new MobEffectInstance(ModMobEffects.WITHER_IMMUNITY, SUMMON_DURATION_TICKS, 0, false, false));
        serverLevel.addFreshEntity(summon);

        serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 0.7f, 0.9f + mob.getRandom().nextFloat() * 0.2f);

        mob.addOwnedRevenantUuid(summon.getUUID());
        mana.setMana(mana.getCurrentMana() - MANA_COST);
        mob.setCastCooldownTicks(MageSkeletonCastGoal.COOLDOWN_TICKS);
        mob.setSummonCooldownTicks(SUMMON_COOLDOWN_TICKS);
    }
}
