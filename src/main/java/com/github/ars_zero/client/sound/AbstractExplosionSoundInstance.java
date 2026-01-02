package com.github.ars_zero.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;

public abstract class AbstractExplosionSoundInstance extends AbstractTickableSoundInstance {
  protected double x;
  protected double y;
  protected double z;
  protected static final double MAX_DISTANCE = 75.0;
  protected static final float BASE_VOLUME = 1.0f;

  protected AbstractExplosionSoundInstance(double x, double y, double z, net.minecraft.sounds.SoundEvent soundEvent,
      net.minecraft.util.RandomSource random) {
    super(soundEvent, SoundSource.NEUTRAL, random);
    this.x = x;
    this.y = y;
    this.z = z;
    this.volume = 0.0f;
    this.pitch = 1.0f;
    this.attenuation = Attenuation.NONE;
  }

  protected void updateVolumeFromDistance() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null) {
      this.volume = 0.0f;
      return;
    }

    double dx = mc.player.getX() - this.x;
    double dy = mc.player.getY() - this.y;
    double dz = mc.player.getZ() - this.z;

    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

    double t = distance / MAX_DISTANCE;

    float attenuated = (float) Math.max(0.0, 1.0 - t * t);

    this.volume = attenuated * BASE_VOLUME;
  }

  @Override
  public double getX() {
    return this.x;
  }

  @Override
  public double getY() {
    return this.y;
  }

  @Override
  public double getZ() {
    return this.z;
  }

  @Override
  public boolean canStartSilent() {
    return true;
  }
}
