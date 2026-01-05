package com.github.ars_zero.common.entity.water;

import com.github.ars_zero.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

public final class WaterConvergenceParticleHelper {

  private static final int RAIN_PARTICLES_PER_RADIUS = 8;
  private static final int SPLASH_PARTICLE_COUNT = 8;

  private WaterConvergenceParticleHelper() {
  }

  public static void spawnRainParticles(ServerLevel serverLevel, BlockPos sphereCenter, int radius) {
    int r2 = radius * radius;
    int particleCount = Math.max(1, radius * RAIN_PARTICLES_PER_RADIUS);

    for (int i = 0; i < particleCount; i++) {
      double dx = (serverLevel.random.nextDouble() - 0.5) * 2.0 * radius;
      double dz = (serverLevel.random.nextDouble() - 0.5) * 2.0 * radius;

      double dist2 = dx * dx + dz * dz;
      if (dist2 > r2) {
        continue;
      }

      double x = sphereCenter.getX() + dx;
      double y = sphereCenter.getY() + 0.5;
      double z = sphereCenter.getZ() + dz;

      double velX = (serverLevel.random.nextDouble() - 0.5) * 0.02;
      double velY = -0.2;
      double velZ = (serverLevel.random.nextDouble() - 0.5) * 0.02;

      serverLevel.sendParticles(ParticleTypes.FALLING_WATER, x, y, z, 1, velX, velY, velZ, 0.0);
    }
  }

  public static void spawnSplashParticle(ServerLevel serverLevel, BlockPos pos) {
    double x = pos.getX() + 0.5;
    double y = pos.getY() + 0.5;
    double z = pos.getZ() + 0.5;
    serverLevel.sendParticles(ModParticles.FAST_POOF.get(), x, y, z, SPLASH_PARTICLE_COUNT, 0.25, 0.25, 0.25, 0.01);
  }
}
