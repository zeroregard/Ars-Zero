package com.github.ars_zero.common.entity.explosion;

import com.github.ars_zero.common.explosion.ExplosionWorkList;
import com.github.ars_zero.common.util.BlockImmutabilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ExplosionProcessHelper {

    private static final int UPDATE_FLAGS = net.minecraft.world.level.block.Block.UPDATE_CLIENTS;
    private static final double IGNITION_MULTIPLIER = 0.3;

    public static double calculateRadius(float charge, double firePower, int aoeLevel, int dampenLevel) {
        double radius = charge * (14.0 + firePower / 2.0);
        radius += aoeLevel;
        radius -= 0.5 * dampenLevel;
        return Math.max(0.0, radius);
    }

    public static float calculateAdjustedDamage(float baseDamage, int amplifyLevel, int dampenLevel, float charge,
            double firePower) {
        float damage = (baseDamage + amplifyLevel) * charge;
        damage += (float) firePower * charge;
        damage -= 0.5f * dampenLevel * charge;
        return Math.max(0.0f, damage);
    }

    public static float calculateAdjustedPower(float powerMultiplier, float charge) {
        return powerMultiplier * charge * 2.0f;
    }

    public static int calculateExplodeAnimationDurationTicks(double radius, double baseRadiusForTiming,
            double targetRadiusForTiming, double baseAnimationSeconds, double targetAnimationSeconds) {
        double animationSeconds;
        if (radius <= baseRadiusForTiming) {
            animationSeconds = baseAnimationSeconds;
        } else {
            double baseRadiusCubed = baseRadiusForTiming * baseRadiusForTiming * baseRadiusForTiming;
            double targetRadiusCubed = targetRadiusForTiming * targetRadiusForTiming * targetRadiusForTiming;
            double cubicScaleFactor = (targetAnimationSeconds - baseAnimationSeconds)
                    / (targetRadiusCubed - baseRadiusCubed);
            double radiusCubed = radius * radius * radius;
            animationSeconds = baseAnimationSeconds + (radiusCubed - baseRadiusCubed) * cubicScaleFactor;
        }
        return (int) (animationSeconds * 20.0);
    }

    public static boolean shouldDestroyBlock(ServerLevel level, BlockPos pos, BlockState state, int distanceSquared,
            Vec3 explosionCenter, double explosionRadius, double firePower) {
        if (explosionCenter == null || explosionRadius <= 0) {
            return true;
        }

        double distance = Math.sqrt(distanceSquared);
        double normalizedDistance = Math.min(1.0, distance / explosionRadius);
        float hardness = state.getDestroySpeed(level, pos);

        if (hardness <= 3.5f) {
            return calculateSoftBlockChance(level, normalizedDistance, hardness, firePower);
        }

        return calculateHardBlockChance(level, normalizedDistance, hardness, firePower);
    }

    private static boolean calculateSoftBlockChance(ServerLevel level, double normalizedDistance, float hardness,
            double firePower) {
        if (normalizedDistance <= 0.75) {
            return true;
        }

        double edgeDistance = (normalizedDistance - 0.75) / 0.25;
        double minChanceAtEdge = 0.70 - ((hardness - 1.5f) / 2.0f) * 0.30;

        double firePowerBonus = calculateFirePowerBonus(hardness, firePower);
        minChanceAtEdge = Math.min(1.0, minChanceAtEdge + firePowerBonus);

        double finalChance = 1.0 - (edgeDistance * (1.0 - minChanceAtEdge));
        return level.getRandom().nextDouble() < finalChance;
    }

    private static boolean calculateHardBlockChance(ServerLevel level, double normalizedDistance, float hardness,
            double firePower) {
        double distanceFactor = 1.0 - normalizedDistance;
        double hardnessResistance = Math.min(0.80, hardness / 200.0);
        double baseChance = distanceFactor * (1.0 - hardnessResistance);

        double firePowerBonus = calculateFirePowerBonus(hardness, firePower);
        baseChance = Math.min(1.0, baseChance + firePowerBonus);

        double minChance = getMinimumChance(hardness);
        double finalChance = Math.max(minChance, baseChance);

        return level.getRandom().nextDouble() < finalChance;
    }

    private static double calculateFirePowerBonus(float hardness, double firePower) {
        if (firePower <= 0.0) {
            return 0.0;
        }

        double normalizedFirePower = Math.min(1.0, firePower / 10.0);

        if (hardness <= 3.5f) {
            double softBlockBonus = normalizedFirePower * 0.60;
            if (hardness <= 2.0f) {
                softBlockBonus *= 1.2;
            }
            return softBlockBonus;
        } else {
            double hardBlockBonus = normalizedFirePower * 0.15;
            if (hardness >= 50.0f) {
                hardBlockBonus *= 0.3;
            } else if (hardness >= 20.0f) {
                hardBlockBonus *= 0.5;
            }
            return hardBlockBonus;
        }
    }

    private static double getMinimumChance(float hardness) {
        if (hardness <= 5.0f) {
            return 0.50;
        } else if (hardness <= 10.0f) {
            return 0.30;
        }
        return 0.0;
    }

    private static double calculateFireIgnitionChance(double firePower) {
        if (firePower <= 0.0) {
            return 0.0;
        }
        double normalizedFirePower = Math.min(1.0, firePower / 10.0);
        return normalizedFirePower * 0.50 * IGNITION_MULTIPLIER;
    }

    private static boolean isBlockExposedToAir(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            if (adjacentState.isAir()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLowHardness(ServerLevel level, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(level, pos);
        return hardness > 0.0f && hardness <= 1.0f;
    }

    private static void tryIgniteExposedNearbyBlocks(ServerLevel serverLevel, BlockPos destroyedPos,
            double explosionRadius, double firePower) {
        for (Direction direction : Direction.values()) {
            BlockPos checkPos = destroyedPos.relative(direction);

            if (serverLevel.isOutsideBuildHeight(checkPos)) {
                continue;
            }

            BlockState state = serverLevel.getBlockState(checkPos);
            if (state.isAir() || BlockImmutabilityUtil.isBlockImmutable(state)) {
                continue;
            }
            if (state.getDestroySpeed(serverLevel, checkPos) < 0.0f) {
                continue;
            }

            if (!isBlockExposedToAir(serverLevel, checkPos)) {
                continue;
            }

            double fireChance = calculateFireIgnitionChance(firePower);
            if (fireChance > 0.0 && serverLevel.getRandom().nextDouble() < fireChance) {
                BlockPos firePos = checkPos.above();
                if (serverLevel.isOutsideBuildHeight(firePos)) {
                    continue;
                }
                BlockState firePosState = serverLevel.getBlockState(firePos);
                if (!firePosState.isAir()) {
                    continue;
                }

                boolean useSoulFire = firePower >= 10.0;
                if (useSoulFire && hasLowHardness(serverLevel, checkPos, state)) {
                    serverLevel.setBlock(checkPos, Blocks.SOUL_SAND.defaultBlockState(), UPDATE_FLAGS);
                }

                BlockState fireState = useSoulFire ? Blocks.SOUL_FIRE.defaultBlockState()
                        : Blocks.FIRE.defaultBlockState();
                if (fireState.canSurvive(serverLevel, firePos)) {
                    serverLevel.setBlock(firePos, fireState, UPDATE_FLAGS);
                } else if (useSoulFire) {
                    BlockState regularFireState = Blocks.FIRE.defaultBlockState();
                    if (regularFireState.canSurvive(serverLevel, firePos)) {
                        serverLevel.setBlock(firePos, regularFireState, UPDATE_FLAGS);
                    }
                }
            }
        }
    }

    public static class ProcessResult {
        public final int nextWorkIndex;
        public final int deferredSize;
        public final long[] deferredPositions;
        public final boolean shouldDiscard;

        public ProcessResult(int nextWorkIndex, int deferredSize, long[] deferredPositions, boolean shouldDiscard) {
            this.nextWorkIndex = nextWorkIndex;
            this.deferredSize = deferredSize;
            this.deferredPositions = deferredPositions;
            this.shouldDiscard = shouldDiscard;
        }
    }

    public static ProcessResult processWorkList(
            ServerLevel serverLevel,
            Entity entity,
            ExplosionWorkList workList,
            int nextWorkIndex,
            Vec3 explosionCenter,
            double explosionRadius,
            long[] deferredPositions,
            int deferredSize,
            int maxPerTick,
            double firePower) {

        int remaining = workList.size() - nextWorkIndex;
        int budget = Math.min(maxPerTick, remaining);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < budget; i++) {
            long packedPos = workList.positionAt(nextWorkIndex);
            int distSq = workList.distanceSquaredAt(nextWorkIndex);
            nextWorkIndex++;

            pos.set(packedPos);
            if (serverLevel.isOutsideBuildHeight(pos)) {
                continue;
            }

            BlockState state = serverLevel.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (BlockImmutabilityUtil.isBlockImmutable(state)) {
                continue;
            }

            boolean useSoulFire = firePower >= 10.0;
            if (useSoulFire && state.getBlock() == Blocks.WATER) {
                serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                continue;
            }

            if (state.getDestroySpeed(serverLevel, pos) < 0.0f) {
                continue;
            }

            if (!shouldDestroyBlock(serverLevel, pos, state, distSq, explosionCenter, explosionRadius, firePower)) {
                continue;
            }

            float hardness = state.getDestroySpeed(serverLevel, pos);
            double dropChance = Math.min(1.0, hardness / 10.0);
            if (serverLevel.getRandom().nextDouble() < dropChance) {
                Block.dropResources(state, serverLevel, pos, null, entity, ItemStack.EMPTY);
            }

            boolean removed = serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
            if (removed) {
                double fireChance = calculateFireIgnitionChance(firePower);
                if (fireChance > 0.0 && serverLevel.getRandom().nextDouble() < fireChance) {
                    BlockPos belowPos = pos.below();
                    if (useSoulFire && !serverLevel.isOutsideBuildHeight(belowPos)) {
                        BlockState belowState = serverLevel.getBlockState(belowPos);
                        if (hasLowHardness(serverLevel, belowPos, belowState)) {
                            serverLevel.setBlock(belowPos, Blocks.SOUL_SAND.defaultBlockState(), UPDATE_FLAGS);
                        }
                    }
                    BlockState fireState = useSoulFire ? Blocks.SOUL_FIRE.defaultBlockState()
                            : Blocks.FIRE.defaultBlockState();
                    if (fireState.canSurvive(serverLevel, pos)) {
                        serverLevel.setBlock(pos, fireState, UPDATE_FLAGS);
                    } else if (useSoulFire) {
                        BlockState regularFireState = Blocks.FIRE.defaultBlockState();
                        if (regularFireState.canSurvive(serverLevel, pos)) {
                            serverLevel.setBlock(pos, regularFireState, UPDATE_FLAGS);
                        }
                    }
                }
                tryIgniteExposedNearbyBlocks(serverLevel, pos, explosionRadius, firePower);
                if (serverLevel.getRandom().nextDouble() < 0.10) {
                    ExplosionParticleHelper.spawnSmokeResidue(serverLevel, pos);
                }
            }
            if (!removed) {
                deferredPositions = deferPosition(deferredPositions, deferredSize, packedPos);
                deferredSize++;
            }
        }

        boolean shouldDiscard = nextWorkIndex >= workList.size();
        return new ProcessResult(nextWorkIndex, deferredSize, deferredPositions, shouldDiscard);
    }

    public static long[] deferPosition(long[] deferredPositions, int deferredSize, long packedPos) {
        if (deferredPositions == null) {
            deferredPositions = new long[1024];
        }
        if (deferredSize >= deferredPositions.length) {
            long[] next = new long[deferredPositions.length + (deferredPositions.length >> 1)];
            System.arraycopy(deferredPositions, 0, next, 0, deferredSize);
            deferredPositions = next;
        }
        deferredPositions[deferredSize] = packedPos;
        return deferredPositions;
    }

    public static ExplosionWorkList rollDeferredIntoWork(long[] deferredPositions, int deferredSize) {
        ExplosionWorkList list = new ExplosionWorkList(deferredSize);
        for (int i = 0; i < deferredSize; i++) {
            list.add(deferredPositions[i], 0);
        }
        return list;
    }
}
