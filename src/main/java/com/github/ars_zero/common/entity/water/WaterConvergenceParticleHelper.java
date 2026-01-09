package com.github.ars_zero.common.entity.water;

import com.github.ars_zero.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class WaterConvergenceParticleHelper {

  private static final int SPLASH_PARTICLE_COUNT = 16;

  private WaterConvergenceParticleHelper() {
  }

  public static void spawnSplashParticle(ServerLevel serverLevel, BlockPos pos) {
    double x = pos.getX() + 0.5;
    double y = pos.getY() + 0.5;
    double z = pos.getZ() + 0.5;
    serverLevel.sendParticles(ModParticles.FAST_POOF.get(), x, y, z, SPLASH_PARTICLE_COUNT, 0.25, 0, 0.25, 0.01);
  }
}
