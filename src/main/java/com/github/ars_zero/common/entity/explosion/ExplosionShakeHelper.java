package com.github.ars_zero.common.entity.explosion;

import com.github.ars_zero.common.network.PacketExplosionShake;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class ExplosionShakeHelper {

  /**
   * Sends screen shake packets to nearby players based on their distance from the
   * explosion.
   * 
   * @param level  The server level
   * @param center The center position of the explosion
   * @param radius The explosion radius
   */
  public static void shakeNearbyPlayers(ServerLevel level, Vec3 center, double radius) {
    double shakeRange = radius * 4.0;
    double shakeRangeSq = shakeRange * shakeRange;

    for (var player : level.players()) {
      if (!(player instanceof ServerPlayer serverPlayer)) {
        continue;
      }
      double distanceSq = player.distanceToSqr(center.x, center.y, center.z);
      if (distanceSq <= shakeRangeSq) {
        double distance = Math.sqrt(distanceSq);
        float intensity;

        if (distance <= radius) {
          // Within explosion radius: intensity from 1.7 to 0.3 (doubled from 0.85 to
          // 0.15)
          double normalizedDistance = distance / radius;
          intensity = (float) ((1.0 - normalizedDistance) * 1.4 + 0.3);
        } else {
          // Between radius and radius*4: intensity from 0.3 to 0.1 (doubled from 0.15 to
          // 0.05)
          double outerDistance = distance - radius;
          double outerRange = radius * 3.0;
          double normalizedOuterDistance = Math.min(1.0, outerDistance / outerRange);
          intensity = (float) ((1.0 - normalizedOuterDistance) * 0.3);
          intensity = Math.max(0.1f, intensity);
        }

        int durationTicks = Math.max(20, (int) (radius * 1.0));
        durationTicks = Math.min(durationTicks, 80);

        PacketExplosionShake packet = new PacketExplosionShake(intensity, durationTicks);
        PacketDistributor.sendToPlayer(serverPlayer, packet);
      }
    }
  }
}
