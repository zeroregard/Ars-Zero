package com.github.ars_zero.common.entity.ai;

import com.github.ars_zero.api.spell.MobSpellBehaviour;
import com.github.ars_zero.common.entity.MageSkeletonEntity;
import com.hollingsworth.arsnouveau.api.entity.ISummon;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import alexthw.ars_elemental.common.entity.summon.SummonUndead;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Summons one SummonUndead (Ars-Elemental) as an ally. Only runs when the caster has no living
 * skeleton summon nearby. Lower priority than blight voxel (register after BlightVoxelPushSpellBehaviour).
 */
public class SummonUndeadSpellBehaviour implements MobSpellBehaviour {

    /** Base duration in ticks (15 seconds). */
    private static final int SUMMON_DURATION_TICKS = 15 * 20;
    private static final int MANA_COST = 150;
    /** Radius to check for existing owned summons. */
    private static final double SUMMON_CHECK_RADIUS = 24.0;

    @Override
    public int getManaCost() {
        return MANA_COST;
    }

    @Override
    public boolean canRun(Mob caster, LivingEntity target) {
        return !hasOwnedSkeletonSummon(caster);
    }

    @Override
    public boolean run(Mob caster, LivingEntity target) {
        Level level = caster.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        var mana = CapabilityRegistry.getMana(caster);
        if (mana == null || mana.getCurrentMana() < MANA_COST) {
            return false;
        }

        SummonUndead summon = new SummonUndead(serverLevel);
        summon.setWeapon(new ItemStack(Items.IRON_SWORD));
        summon.setOwner(caster);
        summon.setLimitedLife(SUMMON_DURATION_TICKS);

        int dx = caster.getRandom().nextInt(5) - 2;
        int dz = caster.getRandom().nextInt(5) - 2;
        double x = caster.getX() + dx;
        double y = caster.getY();
        double z = caster.getZ() + dz;
        summon.moveTo(x, y, z, 0.0F, 0.0F);
        summon.finalizeSpawn((ServerLevelAccessor) serverLevel, serverLevel.getCurrentDifficultyAt(summon.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        serverLevel.addFreshEntity(summon);

        mana.setMana(mana.getCurrentMana() - MANA_COST);
        if (caster instanceof MageSkeletonEntity mage) {
            mage.setCastCooldownTicks(MageSkeletonCastGoal.COOLDOWN_TICKS);
        }
        return true;
    }

    /**
     * Returns true if the caster already has a living skeleton-type summon (SummonSkeleton or SummonUndead) owned by them.
     */
    private static boolean hasOwnedSkeletonSummon(Mob caster) {
        Level level = caster.level();
        if (level == null) {
            return false;
        }
        UUID ownerUuid = caster.getUUID();
        AABB box = caster.getBoundingBox().inflate(SUMMON_CHECK_RADIUS);
        for (Entity entity : level.getEntities(caster, box, e -> e instanceof LivingEntity living && living.isAlive())) {
            if (entity instanceof ISummon summon && ownerUuid.equals(summon.getOwnerUUID())) {
                if (entity instanceof Skeleton) {
                    return true;
                }
            }
        }
        return false;
    }
}
