package com.github.ars_zero.common.entity.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ExplosionHelper {
    
    public static double calculateRadius(float charge, double firePower, int aoeLevel, int dampenLevel) {
        double radius = charge * (14.0 + 3.0 * firePower);
        radius += aoeLevel;
        radius -= 0.5 * dampenLevel;
        return Math.max(0.0, radius);
    }
    
    public static float calculateAdjustedDamage(float baseDamage, int amplifyLevel, int dampenLevel, float charge) {
        float damage = (baseDamage + amplifyLevel) * charge;
        damage -= 0.5f * dampenLevel * charge;
        return Math.max(0.0f, damage);
    }
    
    public static float calculateAdjustedPower(float powerMultiplier, float charge) {
        return powerMultiplier * charge * 2.0f;
    }
    
    public static int calculateExplodeAnimationDurationTicks(double radius, double baseRadiusForTiming, double targetRadiusForTiming, double baseAnimationSeconds, double targetAnimationSeconds) {
        double animationSeconds;
        if (radius <= baseRadiusForTiming) {
            animationSeconds = baseAnimationSeconds;
        } else {
            double baseRadiusCubed = baseRadiusForTiming * baseRadiusForTiming * baseRadiusForTiming;
            double targetRadiusCubed = targetRadiusForTiming * targetRadiusForTiming * targetRadiusForTiming;
            double cubicScaleFactor = (targetAnimationSeconds - baseAnimationSeconds) / (targetRadiusCubed - baseRadiusCubed);
            double radiusCubed = radius * radius * radius;
            animationSeconds = baseAnimationSeconds + (radiusCubed - baseRadiusCubed) * cubicScaleFactor;
        }
        return (int) (animationSeconds * 20.0);
    }
    
    public static boolean shouldDestroyBlock(ServerLevel level, BlockPos pos, BlockState state, int distanceSquared, Vec3 explosionCenter, double explosionRadius) {
        if (explosionCenter == null || explosionRadius <= 0) {
            return true;
        }

        double distance = Math.sqrt(distanceSquared);
        double normalizedDistance = Math.min(1.0, distance / explosionRadius);
        float hardness = state.getDestroySpeed(level, pos);
        
        if (hardness <= 3.5f) {
            return calculateSoftBlockChance(level, normalizedDistance, hardness);
        }
        
        return calculateHardBlockChance(level, normalizedDistance, hardness);
    }
    
    private static boolean calculateSoftBlockChance(ServerLevel level, double normalizedDistance, float hardness) {
        if (normalizedDistance <= 0.75) {
            return true;
        }
        
        double edgeDistance = (normalizedDistance - 0.75) / 0.25;
        double minChanceAtEdge = 0.70 - ((hardness - 1.5f) / 2.0f) * 0.30;
        double finalChance = 1.0 - (edgeDistance * (1.0 - minChanceAtEdge));
        return level.getRandom().nextDouble() < finalChance;
    }
    
    private static boolean calculateHardBlockChance(ServerLevel level, double normalizedDistance, float hardness) {
        double distanceFactor = 1.0 - normalizedDistance;
        double hardnessResistance = Math.min(0.80, hardness / 200.0);
        double baseChance = distanceFactor * (1.0 - hardnessResistance);
        
        double minChance = getMinimumChance(hardness);
        double finalChance = Math.max(minChance, baseChance);
        
        return level.getRandom().nextDouble() < finalChance;
    }
    
    private static double getMinimumChance(float hardness) {
        if (hardness <= 5.0f) {
            return 0.50;
        } else if (hardness <= 10.0f) {
            return 0.30;
        }
        return 0.0;
    }
}

