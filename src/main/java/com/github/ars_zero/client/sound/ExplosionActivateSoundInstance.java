package com.github.ars_zero.client.sound;

import com.github.ars_zero.registry.ModSounds;
import net.minecraft.client.Minecraft;

public class ExplosionActivateSoundInstance extends AbstractExplosionSoundInstance {
  private int age = 0;
  private static final int MAX_AGE = 200;

  public ExplosionActivateSoundInstance(double x, double y, double z) {
    super(x, y, z, ModSounds.EXPLOSION_ACTIVATE.get(), Minecraft.getInstance().level.getRandom());
    this.looping = false;
    updateVolumeFromDistance();
  }

  @Override
  public void tick() {
    this.age++;
    if (this.age >= MAX_AGE) {
      this.stop();
      return;
    }
    updateVolumeFromDistance();
  }

  @Override
  public boolean canPlaySound() {
    return true;
  }
}
