package com.github.ars_zero.common.entity.explosion;

import com.github.ars_zero.common.particle.ExplosiveChargeParticleOptions;
import com.github.ars_zero.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

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

  public static void spawnSpiralParticles(ServerLevel level, Vec3 center, float charge, int tickCount,
      double firePower) {
    double spiralRadius = 3.0 + charge * 4.0;
    double time = tickCount * 0.1;

    ExplosiveChargeParticleOptions baseOptions = getChargeParticleOptions(firePower);
    ExplosiveChargeParticleOptions particleOptions = new ExplosiveChargeParticleOptions(
        ModParticles.EXPLOSIVE_CHARGE.get(), baseOptions.r, baseOptions.g, baseOptions.b);

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

}
