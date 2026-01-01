package com.github.ars_zero.common.entity.explosion;

import com.github.ars_zero.client.sound.ExplosionChargeSoundInstance;
import com.github.ars_zero.client.sound.ExplosionIdleSoundInstance;
import com.github.ars_zero.client.sound.ExplosionPrimingSoundInstance;
import com.github.ars_zero.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.server.level.ServerLevel;
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

  public static void playActivateSound(ServerLevel level, double x, double y, double z) {
    level.playSound(null, x, y, z, ModSounds.EXPLOSION_ACTIVATE.get(), SoundSource.NEUTRAL, 20.0f, 1.0f);
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
}
