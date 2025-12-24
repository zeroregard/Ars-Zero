package com.github.ars_zero.common.explosion;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class LargeExplosionDamage {
    private LargeExplosionDamage() {
    }

    public static void apply(ServerLevel level, Entity source, Vec3 center, double radius, float baseDamage, float powerMultiplier) {
        if (radius <= 0.0) {
            return;
        }

        AABB aabb = new AABB(center, center).inflate(radius);
        List<Entity> entities = level.getEntities(source, aabb, Entity::isAlive);

        DamageSource damageSource = level.damageSources().explosion(source, source);
        double maxDamage = (double) Math.max(0.0f, baseDamage) * (double) Math.max(0.0f, powerMultiplier);
        double maxKnockback = 0.9 * (double) Math.max(0.0f, powerMultiplier);

        for (Entity entity : entities) {
            Vec3 delta = entity.position().subtract(center);
            double dist = delta.length();
            if (dist > radius || dist <= 0.0001) {
                continue;
            }

            double t = 1.0 - (dist / radius);
            double damage = maxDamage * t;
            if (damage > 0.0 && entity instanceof LivingEntity living) {
                living.hurt(damageSource, (float) damage);
            }

            Vec3 knockDir = delta.scale(1.0 / dist);
            double knock = maxKnockback * t;
            entity.push(knockDir.x * knock, knockDir.y * knock * 0.6, knockDir.z * knock);
            entity.hurtMarked = true;
        }
    }
}

