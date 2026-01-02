package com.github.ars_zero.client.sound;

import com.github.ars_zero.common.entity.explosion.ExplosionControllerEntity;
import com.github.ars_zero.registry.ModSounds;

public class ExplosionChargeSoundInstance extends AbstractExplosionSoundInstance {
  private final ExplosionControllerEntity entity;
  private static final double DEFAULT_CHARGE_TIME_SECONDS = 4.0;

  public ExplosionChargeSoundInstance(ExplosionControllerEntity entity) {
    super(entity.getX(), entity.getY(), entity.getZ(), ModSounds.EXPLOSION_CHARGE.get(), entity.level().getRandom());
    this.entity = entity;
    this.looping = false;
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

      double chargeTimeSeconds = this.entity.calculateChargeTimeSeconds();
      float chargePitch = (float) (DEFAULT_CHARGE_TIME_SECONDS / chargeTimeSeconds);
      this.pitch = Math.max(0.5f, Math.min(2.0f, chargePitch));
    }

    updateVolumeFromDistance();
  }

  @Override
  public boolean canPlaySound() {
    return this.entity != null && this.entity.isAlive() && !this.entity.isExploding();
  }
}
