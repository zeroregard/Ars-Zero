package com.github.ars_zero.client.sound;

import com.github.ars_zero.common.entity.explosion.ExplosionControllerEntity;
import com.github.ars_zero.registry.ModSounds;

public class ExplosionIdleSoundInstance extends AbstractExplosionSoundInstance {
  private final ExplosionControllerEntity entity;
  private static final double DEFAULT_IDLE_TIME_SECONDS = 2.0;
  private static final int FADE_START_TICKS = 19;

  public ExplosionIdleSoundInstance(ExplosionControllerEntity entity) {
    super(entity.getX(), entity.getY(), entity.getZ(), ModSounds.EXPLOSION_IDLE.get(), entity.level().getRandom());
    this.entity = entity;
    this.looping = true;
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

      int remainingLifespan = this.entity.getLifespan();

      updateVolumeFromDistance();

      if (remainingLifespan <= FADE_START_TICKS) {
        float fadeProgress = Math.max(0.0f, (float) remainingLifespan / (float) FADE_START_TICKS);
        this.volume *= fadeProgress;

        if (this.volume <= 0.0f) {
          this.stop();
          return;
        }
      }

      double idleTimeSeconds = this.entity.calculateIdleTimeSeconds();
      int maxLifespan = this.entity.getMaxLifespan();
      double lifespanSpeedMultiplier = this.entity.calculateLifespanSpeedMultiplier(remainingLifespan, maxLifespan);

      float basePitch = (float) (DEFAULT_IDLE_TIME_SECONDS / idleTimeSeconds);
      float idlePitch = (float) (basePitch * lifespanSpeedMultiplier);
      this.pitch = Math.max(1f, Math.min(16.0f, idlePitch));
    }
  }

  @Override
  public boolean canPlaySound() {
    return this.entity != null && this.entity.isAlive() && !this.entity.isExploding();
  }
}
