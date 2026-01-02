package com.github.ars_zero.common.entity.explosion;

import com.github.ars_zero.client.sound.ExplosionChargeSoundInstance;
import com.github.ars_zero.client.sound.ExplosionIdleSoundInstance;
import com.github.ars_zero.client.sound.ExplosionPrimingSoundInstance;
import com.github.ars_zero.client.sound.ExplosionResolverSoundInstance;
import net.minecraft.sounds.SoundEvent;
import com.github.ars_zero.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ExplosionSoundHelper {

  @OnlyIn(Dist.CLIENT)
  public static void startChargeSound(ExplosionControllerEntity entity, Object[] chargeSoundInstanceRef) {
    if (entity.level().isClientSide) {
      ExplosionChargeSoundInstance soundInstance = new ExplosionChargeSoundInstance(entity);
      chargeSoundInstanceRef[0] = soundInstance;
      Minecraft.getInstance().getSoundManager().play(soundInstance);
    }
  }

  @OnlyIn(Dist.CLIENT)
  public static void stopChargeSound(Object[] chargeSoundInstanceRef) {
    if (chargeSoundInstanceRef[0] != null) {
      SoundInstance sound = (SoundInstance) chargeSoundInstanceRef[0];
      Minecraft.getInstance().getSoundManager().stop(sound);
      chargeSoundInstanceRef[0] = null;
    }
  }

  public static void playActivateSound(ServerLevel level, double x, double y, double z, double radius) {
    // Send activation sound packet to nearby players for custom sound instance with
    // smooth attenuation
    // Custom sound instance provides smooth distance-based volume falloff up to 100
    // blocks
    double soundRange = 100.0; // Match AbstractExplosionSoundInstance.MAX_DISTANCE
    com.github.ars_zero.common.network.PacketExplosionActivateSound activatePacket = new com.github.ars_zero.common.network.PacketExplosionActivateSound(
        x, y, z);

    for (var player : level.players()) {
      if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
        double distanceSq = player.distanceToSqr(x, y, z);
        if (distanceSq <= soundRange * soundRange) {
          net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, activatePacket);
        }
      }
    }

    // Still use playSound for distant sound (long range, no attenuation needed)
    level.playSound(null, x, y, z, ModSounds.EXPLOSION_DISTANT.get(), SoundSource.NEUTRAL, 10.0f, 1.0f);
  }

  public static void playRingExplodeSound(ServerLevel level, double x, double y, double z) {
    level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 1.0f, 1.0f);
  }

  @OnlyIn(Dist.CLIENT)
  public static void startIdleSound(ExplosionControllerEntity entity, Object[] idleSoundInstanceRef) {
    if (entity.level().isClientSide) {
      ExplosionIdleSoundInstance soundInstance = new ExplosionIdleSoundInstance(entity);
      idleSoundInstanceRef[0] = soundInstance;
      Minecraft.getInstance().getSoundManager().play(soundInstance);
    }
  }

  @OnlyIn(Dist.CLIENT)
  public static void stopIdleSound(Object[] idleSoundInstanceRef) {
    if (idleSoundInstanceRef[0] != null) {
      SoundInstance sound = (SoundInstance) idleSoundInstanceRef[0];
      Minecraft.getInstance().getSoundManager().stop(sound);
      idleSoundInstanceRef[0] = null;
    }
  }

  @OnlyIn(Dist.CLIENT)
  public static void startPrimingSound(ExplosionControllerEntity entity, Object[] primingSoundInstanceRef) {
    if (entity.level().isClientSide) {
      ExplosionPrimingSoundInstance soundInstance = new ExplosionPrimingSoundInstance(entity);
      primingSoundInstanceRef[0] = soundInstance;
      Minecraft.getInstance().getSoundManager().play(soundInstance);
    }
  }

  @OnlyIn(Dist.CLIENT)
  public static void stopPrimingSound(Object[] primingSoundInstanceRef) {
    if (primingSoundInstanceRef[0] != null) {
      SoundInstance sound = (SoundInstance) primingSoundInstanceRef[0];
      Minecraft.getInstance().getSoundManager().stop(sound);
      primingSoundInstanceRef[0] = null;
    }
  }

  @OnlyIn(Dist.CLIENT)
  public static void startResolverSound(ExplosionControllerEntity entity, Object[] resolverSoundInstanceRef) {
    if (entity.level().isClientSide) {
      SoundEvent resolveSound = entity.getResolveSound();
      if (resolveSound != null) {
        ExplosionResolverSoundInstance soundInstance = new ExplosionResolverSoundInstance(entity, resolveSound);
        resolverSoundInstanceRef[0] = soundInstance;
        Minecraft.getInstance().getSoundManager().play(soundInstance);
      }
    }
  }

  @OnlyIn(Dist.CLIENT)
  public static void stopResolverSound(Object[] resolverSoundInstanceRef) {
    if (resolverSoundInstanceRef[0] != null) {
      SoundInstance sound = (SoundInstance) resolverSoundInstanceRef[0];
      Minecraft.getInstance().getSoundManager().stop(sound);
      resolverSoundInstanceRef[0] = null;
    }
  }
}
