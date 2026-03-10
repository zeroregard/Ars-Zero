package com.github.ars_zero.common.entity.ai;

import com.github.ars_zero.common.entity.AbstractBlightedSkeleton;
import com.github.ars_zero.registry.ModBlocks;
import net.minecraft.core.BlockPos;
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

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * When undisturbed (no target), walks to the nearest ritual altar and periodically
 * raises vanilla undead near it. Yields immediately when a target is acquired.
 *
 * Pass a custom {@code raiseIntervalTicks} to tune how quickly each tier raises undead.
 */
public class NecromancerRitualGoal extends Goal {

    private static final int ALTAR_SCAN_RADIUS = 16;
    private static final int MAX_RAISED = 3;
    private static final double WALK_SPEED = 0.6;

    private final AbstractBlightedSkeleton mob;
    private final int raiseIntervalTicks;
    @Nullable private BlockPos altarPos;
    private int raiseTicker = 0;

    public NecromancerRitualGoal(AbstractBlightedSkeleton mob, int raiseIntervalTicks) {
        this.mob = mob;
        this.raiseIntervalTicks = raiseIntervalTicks;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null && mob.getTarget().isAlive()) return false;
        BlockPos found = findAltar();
        if (found == null) return false;
        altarPos = found;
        return findBlightedSoilNearAltar() != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.getTarget() != null && mob.getTarget().isAlive()) return false;
        if (altarPos == null) return false;
        if (!mob.level().getBlockState(altarPos).is(ModBlocks.OSSUARY_BEACON.get())) return false;
        return findBlightedSoilNearAltar() != null;
    }

    @Override
    public void stop() {
        altarPos = null;
        raiseTicker = 0;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (altarPos == null) return;

        // Keep arms raised continuously during ritual
        mob.setSpellCastArmsUpTicks(AbstractBlightedSkeleton.SPELL_CAST_ARMS_UP_TICKS);

        // Walk toward the altar
        double dist = mob.blockPosition().distSqr(altarPos);
        if (dist > 9.0) {
            mob.getNavigation().moveTo(altarPos.getX() + 0.5, altarPos.getY(), altarPos.getZ() + 0.5, WALK_SPEED);
        } else {
            mob.getNavigation().stop();
        }

        raiseTicker++;
        if (raiseTicker >= raiseIntervalTicks) {
            raiseTicker = 0;
            tryRaiseUndead();
        }
    }

    private void tryRaiseUndead() {
        Level level = mob.level();
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (altarPos == null) return;
        if (mob.countLivingOwnedRevenants() >= MAX_RAISED) return;

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
        // Dust particles rising from the spawn position — dark green/grey blight colour
        serverLevel.sendParticles(
                new net.minecraft.core.particles.DustParticleOptions(
                        new org.joml.Vector3f(0.29f, 0.48f, 0.19f), 1.2f),
                spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5,
                24, 0.4, 0.6, 0.4, 0.05);
        serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 0.6f, 0.8f + mob.getRandom().nextFloat() * 0.4f);
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
