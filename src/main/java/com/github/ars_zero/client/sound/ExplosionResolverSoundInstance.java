package com.github.ars_zero.client.sound;

import com.github.ars_zero.common.entity.explosion.ExplosionControllerEntity;
import net.minecraft.sounds.SoundEvent;

public class ExplosionResolverSoundInstance extends AbstractExplosionSoundInstance {
  private final ExplosionControllerEntity entity;

  public ExplosionResolverSoundInstance(ExplosionControllerEntity entity, SoundEvent soundEvent) {
    super(entity.getX(), entity.getY(), entity.getZ(), soundEvent, entity.level().getRandom());
    this.entity = entity;
    this.looping = false;
    updateVolumeFromDistance();
  }

  @Override
  public void tick() {
    if (this.entity == null || !this.entity.isAlive() || this.entity.isExploding()) {
      this.stop();
      return;
    }

    if (this.entity.level() != null) {
      this.x = this.entity.getX();
      this.y = this.entity.getY();
      this.z = this.entity.getZ();
    }

    updateVolumeFromDistance();
  }

  @Override
  public boolean canPlaySound() {
    return this.entity != null && this.entity.isAlive() && !this.entity.isExploding();
  }
}
