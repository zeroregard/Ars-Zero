package com.github.ars_zero.common.entity.ai;

import com.github.ars_zero.common.block.OssuaryBeaconBlockEntity;
import com.github.ars_zero.common.entity.AbstractBlightedSkeleton;
import com.github.ars_zero.registry.ModBlocks;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * When undisturbed (no target), walks to the nearest ritual altar and periodically
 * raises vanilla undead near it. Yields immediately when a target is acquired.
 */
public class NecromancerRitualGoal extends Goal {

    private static final int ALTAR_SCAN_RADIUS = 10;
    private static final int RAISE_INTERVAL_TICKS = 400;
    private static final int MAX_RAISED = 3;
    private static final double WALK_SPEED = 0.6;
    /** Stop walking once within 8 blocks (8² = 64). */
    private static final double RITUAL_DIST_SQ = 64.0;
    /** Mana consumed each time an undead is raised. */
    private static final int RITUAL_MANA_COST = 500;
    private static final Vector3f BLIGHT_COLOR = new Vector3f(0.29f, 0.48f, 0.19f);

    private final AbstractBlightedSkeleton mob;
    @Nullable private BlockPos altarPos;
    private int raiseTicker = 0;

    public NecromancerRitualGoal(AbstractBlightedSkeleton mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null && mob.getTarget().isAlive()) return false;
        IManaCap mana = CapabilityRegistry.getMana(mob);
        if (mana == null || mana.getCurrentMana() < RITUAL_MANA_COST) return false;
        BlockPos found = findAltar();
        if (found == null) return false;
        OssuaryBeaconBlockEntity be = getBeaconEntity(found);
        if (be == null || !be.tryRegister(mob.getUUID())) return false;
        altarPos = found;
        return findBlightedSoilNearAltar() != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.getTarget() != null && mob.getTarget().isAlive()) return false;
        IManaCap mana = CapabilityRegistry.getMana(mob);
        if (mana == null || mana.getCurrentMana() < RITUAL_MANA_COST) return false;
        if (altarPos == null) return false;
        if (!mob.level().getBlockState(altarPos).is(ModBlocks.OSSUARY_BEACON.get())) return false;
        return getBeaconEntity(altarPos) != null && findBlightedSoilNearAltar() != null;
    }

    @Override
    public void stop() {
        if (altarPos != null) {
            OssuaryBeaconBlockEntity be = getBeaconEntity(altarPos);
            if (be != null) be.unregister(mob.getUUID());
        }
        altarPos = null;
        raiseTicker = 0;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (altarPos == null) return;

        // Keep arms raised continuously during ritual
        mob.setSpellCastArmsUpTicks(AbstractBlightedSkeleton.SPELL_CAST_ARMS_UP_TICKS);

        double dist = mob.blockPosition().distSqr(altarPos);

        // Walk toward the altar until within 8 blocks
        if (dist > RITUAL_DIST_SQ) {
            mob.getNavigation().moveTo(altarPos.getX() + 0.5, altarPos.getY(), altarPos.getZ() + 0.5, WALK_SPEED);
        } else {
            mob.getNavigation().stop();
            // Ambient blight particles drifting from the mage
            if (mob.level() instanceof ServerLevel sl) {
                sl.sendParticles(
                        new DustParticleOptions(BLIGHT_COLOR, 0.8f),
                        mob.getX(), mob.getY() + 1.0, mob.getZ(),
                        2, 0.15, 0.15, 0.15, 0.02);
            }
        }

        raiseTicker++;
        if (raiseTicker >= RAISE_INTERVAL_TICKS) {
            raiseTicker = 0;
            tryRaiseUndead();
        }
    }

    private void tryRaiseUndead() {
        Level level = mob.level();
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (altarPos == null) return;
        if (mob.countLivingOwnedRevenants() >= MAX_RAISED) return;

        // Check and consume mana
        IManaCap mana = CapabilityRegistry.getMana(mob);
        if (mana == null || mana.getCurrentMana() < RITUAL_MANA_COST) return;
        mana.setMana(mana.getCurrentMana() - RITUAL_MANA_COST);

        // Spawn near the closest blighted soil to the altar (± small random offset)
        BlockPos soilPos = findBlightedSoilNearAltar();
        if (soilPos == null) return;
        int dx = mob.getRandom().nextInt(5) - 2;
        int dz = mob.getRandom().nextInt(5) - 2;
        BlockPos spawnPos = soilPos.offset(dx, 1, dz);

        // Raise zombie or skeleton randomly
        boolean raiseZombie = mob.getRandom().nextBoolean();
        if (raiseZombie) {
            Zombie zombie = new Zombie(EntityType.ZOMBIE, serverLevel);
            zombie.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, mob.getRandom().nextFloat() * 360f, 0f);
            zombie.finalizeSpawn((ServerLevelAccessor) serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null);
            serverLevel.addFreshEntity(zombie);
            mob.addOwnedRevenantUuid(zombie.getUUID());
        } else {
            Skeleton skeleton = new Skeleton(EntityType.SKELETON, serverLevel);
            skeleton.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, mob.getRandom().nextFloat() * 360f, 0f);
            skeleton.finalizeSpawn((ServerLevelAccessor) serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null);
            serverLevel.addFreshEntity(skeleton);
            mob.addOwnedRevenantUuid(skeleton.getUUID());
        }
        // Dust particles rising from the spawn position
        serverLevel.sendParticles(
                new DustParticleOptions(BLIGHT_COLOR, 1.2f),
                spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5,
                24, 0.4, 0.6, 0.4, 0.05);
        serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 0.6f, 0.8f + mob.getRandom().nextFloat() * 0.4f);
    }

    @Nullable
    private OssuaryBeaconBlockEntity getBeaconEntity(BlockPos pos) {
        BlockEntity be = mob.level().getBlockEntity(pos);
        return be instanceof OssuaryBeaconBlockEntity beacon ? beacon : null;
    }

    @Nullable
    private BlockPos findBlightedSoilNearAltar() {
        if (altarPos == null) return null;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(
                altarPos.offset(-8, -10, -8),
                altarPos.offset(8, 4, 8))) {
            if (mob.level().getBlockState(p).is(ModBlocks.BLIGHTED_SOIL.get())) {
                double d = p.distSqr(altarPos);
                if (d < bestDist) {
                    bestDist = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    @Nullable
    private BlockPos findAltar() {
        BlockPos origin = mob.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(
                origin.offset(-ALTAR_SCAN_RADIUS, -4, -ALTAR_SCAN_RADIUS),
                origin.offset(ALTAR_SCAN_RADIUS, 16, ALTAR_SCAN_RADIUS))) {
            if (mob.level().getBlockState(p).is(ModBlocks.OSSUARY_BEACON.get())) {
                return p.immutable();
            }
        }
        return null;
    }
}
