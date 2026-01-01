package com.github.ars_zero.common.entity.explosion;

import com.github.ars_zero.common.particle.ExplosiveChargeParticleOptions;
import com.github.ars_zero.common.particle.ExplosionBurstParticleOptions;
import com.github.ars_zero.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ExplosionParticleHelper {

  private static final double SOULFIRE_THRESHOLD = 10.0;

  private static ExplosiveChargeParticleOptions getChargeParticleOptions(double firePower) {
    boolean isSoulfire = firePower >= SOULFIRE_THRESHOLD;
    if (isSoulfire) {
      return new ExplosiveChargeParticleOptions(0.3f, 0.7f, 1.0f);
    } else {
      return new ExplosiveChargeParticleOptions(1.0f, 0.5f, 0.0f);
    }
  }

  private static ExplosionBurstParticleOptions getBurstParticleOptions(double firePower) {
    boolean isSoulfire = firePower >= SOULFIRE_THRESHOLD;
    if (isSoulfire) {
      return new ExplosionBurstParticleOptions(0.3f, 0.7f, 1.0f);
    } else {
      return new ExplosionBurstParticleOptions(1.0f, 0.6f, 0.2f);
    }
  }

  public static void spawnSpiralParticles(ServerLevel level, Vec3 center, float charge, int tickCount,
      double firePower) {
    double spiralRadius = 3.0 + charge * 4.0;
    double time = tickCount * 0.1;

    ExplosiveChargeParticleOptions particleOptions = getChargeParticleOptions(firePower);
    particleOptions.setType(ModParticles.EXPLOSIVE_CHARGE.get());

    int particleCount = 2;
    for (int i = 0; i < particleCount; i++) {
      double angle = (time * 2.0 * Math.PI) + (i * 2.0 * Math.PI / particleCount);
      double heightOffset = Math.sin(time * 0.5 + i * 0.3) * 0.5;

      double x = center.x + Math.cos(angle) * spiralRadius;
      double y = center.y + heightOffset;
      double z = center.z + Math.sin(angle) * spiralRadius;

      double speedX = -Math.sin(angle) * 0.08;
      double speedY = Math.cos(time * 0.5 + i * 0.3) * 0.01;
      double speedZ = Math.cos(angle) * 0.08;

      level.sendParticles(particleOptions, x, y, z, 1, speedX, speedY, speedZ, 0.0);
    }
  }

  public static void spawnSmokeResidue(ServerLevel level, BlockPos pos) {
    double x = pos.getX() + 0.5;
    double y = pos.getY() + 0.5;
    double z = pos.getZ() + 0.5;

    double speedX = (level.getRandom().nextDouble() - 0.5) * 0.02;
    double speedY = level.getRandom().nextDouble() * 0.05;
    double speedZ = (level.getRandom().nextDouble() - 0.5) * 0.02;

    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 1, speedX, speedY, speedZ, 0.0);
  }

  public static void spawnExplosionBurst(ServerLevel level, Vec3 center, double radius) {
  }

  @OnlyIn(Dist.CLIENT)
  public static void spawnExplosionBurstClient(ClientLevel level, Vec3 center, double radius, double firePower) {
    double spawnRadius = radius * 0.5f;

    ExplosionBurstParticleOptions particleOptions = getBurstParticleOptions(firePower);
    particleOptions.setType(ModParticles.EXPLOSION_BURST.get());

    for (int i = 0; i < radius * 4; i++) {
      double u = level.random.nextDouble();
      double v = level.random.nextDouble();

      double theta = 2.0 * Math.PI * u;
      double phi = Math.acos(2.0 * v - 1.0);

      double offsetX = Math.sin(phi) * Math.cos(theta) * spawnRadius;
      double offsetY = Math.cos(phi) * spawnRadius;
      double offsetZ = Math.sin(phi) * Math.sin(theta) * spawnRadius;

      double distance = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
      double dirX = offsetX / distance;
      double dirY = offsetY / distance;
      double dirZ = offsetZ / distance;

      double speed = (0.25 + level.random.nextDouble() * 0.45) * 0.25f * radius;

      double vx = dirX * speed;
      double vy = dirY * speed;
      double vz = dirZ * speed;

      vx += level.random.nextGaussian() * 0.02;
      vy += level.random.nextGaussian() * 0.02;
      vz += level.random.nextGaussian() * 0.02;

      level.addParticle(
          particleOptions,
          center.x + offsetX, center.y + offsetY, center.z + offsetZ,
          vx, vy, vz);
    }
  }

  public static void spawnShockwave(ServerLevel level, Vec3 center, double radius) {
    double maxParticles = Math.min(500, 100 + radius * 15.0);
    int ringCount = radius > 15.0 ? 6 : (radius > 8.0 ? 5 : 4);
    int particlesPerRing = (int) (maxParticles / ringCount);

    for (int ring = 0; ring < ringCount; ring++) {
      double ringRadius = radius * (0.1 + ring * 0.18);
      if (ringRadius < 0.5)
        continue;

      double angleStep = (2.0 * Math.PI) / particlesPerRing;
      double heightVariation = Math.min(2.5, radius * 0.25);

      for (int i = 0; i < particlesPerRing; i++) {
        double angle = i * angleStep + (level.getRandom().nextDouble() - 0.5) * 0.15;
        double height = center.y + (level.getRandom().nextDouble() - 0.5) * heightVariation;

        double x = center.x + Math.cos(angle) * ringRadius;
        double z = center.z + Math.sin(angle) * ringRadius;

        double outwardSpeed = 0.5 + level.getRandom().nextDouble() * 0.5;
        double speedX = Math.cos(angle) * outwardSpeed;
        double speedY = (level.getRandom().nextDouble() - 0.5) * 0.15;
        double speedZ = Math.sin(angle) * outwardSpeed;

        level.sendParticles(ParticleTypes.CRIT, x, height, z, 1, speedX, speedY, speedZ, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, height, z, 1, speedX * 0.8, speedY * 0.8, speedZ * 0.8, 0.0);
      }
    }
  }

  @OnlyIn(Dist.CLIENT)
  public static void spawnShockwaveClient(ClientLevel level, Vec3 center, double radius) {
    double maxParticles = Math.min(500, 100 + radius * 15.0);
    int ringCount = radius > 15.0 ? 6 : (radius > 8.0 ? 5 : 4);
    int particlesPerRing = (int) (maxParticles / ringCount);

    for (int ring = 0; ring < ringCount; ring++) {
      double ringRadius = radius * (0.1 + ring * 0.18);
      if (ringRadius < 0.5)
        continue;

      double angleStep = (2.0 * Math.PI) / particlesPerRing;
      double heightVariation = Math.min(2.5, radius * 0.25);

      for (int i = 0; i < particlesPerRing; i++) {
        double angle = i * angleStep + (level.random.nextDouble() - 0.5) * 0.15;
        double height = center.y + (level.random.nextDouble() - 0.5) * heightVariation;

        double x = center.x + Math.cos(angle) * ringRadius;
        double z = center.z + Math.sin(angle) * ringRadius;

        double outwardSpeed = 0.5 + level.random.nextDouble() * 0.5;
        double speedX = Math.cos(angle) * outwardSpeed;
        double speedY = (level.random.nextDouble() - 0.5) * 0.15;
        double speedZ = Math.sin(angle) * outwardSpeed;

        level.addParticle(ParticleTypes.CRIT, x, height, z, speedX, speedY, speedZ);
        level.addParticle(ParticleTypes.LARGE_SMOKE, x, height, z, speedX * 0.8, speedY * 0.8, speedZ * 0.8);
      }
    }
  }
}
