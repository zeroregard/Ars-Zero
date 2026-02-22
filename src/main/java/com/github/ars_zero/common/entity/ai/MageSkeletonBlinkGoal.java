package com.github.ars_zero.common.entity.ai;

import com.github.ars_zero.common.entity.MageSkeletonEntity;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectBlink;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * When a target gets within 3 blocks, the mage skeleton blinks away to a nearby valid spot
 * (standable block with 2 air above), if he has enough mana. Prioritized over casting.
 */
public class MageSkeletonBlinkGoal extends Goal {

    /** Mana cost matching EffectBlink default. */
    private static final int BLINK_MANA_COST = 50;
    /** Cooldown after blinking so he doesn't spam. */
    public static final int BLINK_COOLDOWN_TICKS = 40;
    /** Trigger when target is within this distance (blocks). */
    private static final double TRIGGER_DISTANCE = 3.0;
    /** Min/max distance to search for blink destination from current position. */
    private static final int BLINK_MIN_RADIUS = 6;
    private static final int BLINK_MAX_RADIUS = 12;
    /** Number of angles to try per radius. */
    private static final int ANGLES = 16;
    /** Y offset steps to try (current, above, below). */
    private static final int[] Y_OFFSETS = { 0, 1, -1, 2, -2 };

    private final MageSkeletonEntity mob;

    public MageSkeletonBlinkGoal(MageSkeletonEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public boolean canUse() {
        if (mob.level().isClientSide()) {
            return false;
        }
        if (mob.getBlinkCooldownTicks() > 0 || mob.getChargeTicks() > 0) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distSq = mob.distanceToSqr(target);
        if (distSq > TRIGGER_DISTANCE * TRIGGER_DISTANCE) {
            return false;
        }
        var mana = CapabilityRegistry.getMana(mob);
        if (mana == null || mana.getCurrentMana() < BLINK_MANA_COST) {
            return false;
        }
        return findBlinkDestination() != null;
    }

    @Override
    public void start() {
        Level level = mob.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos dest = findBlinkDestination();
        if (dest == null) {
            return;
        }
        var mana = CapabilityRegistry.getMana(mob);
        if (mana == null || mana.getCurrentMana() < BLINK_MANA_COST) {
            return;
        }
        Vec3 origin = mob.position();
        mana.setMana(mana.getCurrentMana() - BLINK_MANA_COST);
        spawnBlinkParticles(serverLevel, origin.x, origin.y + 1, origin.z);
        EffectBlink.warpEntity(mob, serverLevel, dest);
        spawnBlinkParticles(serverLevel, mob.getX(), mob.getY() + 1, mob.getZ());
        mob.setBlinkCooldownTicks(BLINK_COOLDOWN_TICKS);
    }

    /**
     * Ender-pearl-style teleport burst: portal and reverse-portal particles at the given position.
     */
    private static void spawnBlinkParticles(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.PORTAL, x, y, z, 12, 0.5, 0.5, 0.5, 0.08);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 8, 0.5, 0.5, 0.5, 0.05);
    }

    /**
     * Finds a block position where the mob can stand: standable block below, and 2 air above.
     * Prefers positions farther from the current target.
     */
    @Nullable
    private BlockPos findBlinkDestination() {
        Level level = mob.level();
        LivingEntity target = mob.getTarget();
        if (level == null || target == null) {
            return null;
        }
        BlockPos origin = mob.blockPosition();
        double bestDistSq = -1;
        BlockPos best = null;
        for (int r = BLINK_MIN_RADIUS; r <= BLINK_MAX_RADIUS; r++) {
            for (int a = 0; a < ANGLES; a++) {
                double angle = 2 * Math.PI * a / ANGLES;
                int dx = (int) Math.round(r * Math.cos(angle));
                int dz = (int) Math.round(r * Math.sin(angle));
                for (int dy : Y_OFFSETS) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (!canStandAt(level, candidate)) {
                        continue;
                    }
                    double distSq = target.distanceToSqr(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5);
                    if (best == null || distSq > bestDistSq) {
                        bestDistSq = distSq;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    /**
     * Position is the block the entity's feet occupy (must be non-occluding with 2 air above).
     * The block below must be standable (solid, not liquid).
     */
    private static boolean canStandAt(Level level, BlockPos pos) {
        if (!EffectBlink.isValidTeleport(level, pos)) {
            return false;
        }
        BlockPos floorPos = pos.below();
        BlockState floor = level.getBlockState(floorPos);
        return floor.getFluidState().isEmpty() && floor.blocksMotion();
    }
}
