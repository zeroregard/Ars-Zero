package com.github.ars_zero.client.sound;

import com.github.ars_zero.common.entity.explosion.ExplosionControllerEntity;
import com.github.ars_zero.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;

public class ExplosionIdleSoundInstance extends AbstractTickableSoundInstance {
  private final ExplosionControllerEntity entity;
  private static final double DEFAULT_IDLE_TIME_SECONDS = 2.0;
  private static final float BASE_VOLUME = 2.0f;
  private static final int FADE_START_TICKS = 19;

  public ExplosionIdleSoundInstance(ExplosionControllerEntity entity) {
    super(ModSounds.EXPLOSION_IDLE.get(), SoundSource.NEUTRAL, entity.level().getRandom());
    this.entity = entity;
    this.volume = BASE_VOLUME;
    this.pitch = 1.0f;
    this.looping = true;
    this.attenuation = Attenuation.LINEAR;
    this.x = entity.getX();
    this.y = entity.getY();
    this.z = entity.getZ();
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

      if (remainingLifespan <= FADE_START_TICKS) {
        float fadeProgress = Math.max(0.0f, (float) remainingLifespan / (float) FADE_START_TICKS);
        this.volume = BASE_VOLUME * fadeProgress;

        if (this.volume <= 0.0f) {
          this.stop();
          return;
        }
      } else {
        this.volume = BASE_VOLUME;
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
  public boolean canStartSilent() {
    return true;
  }

  @Override
  public boolean canPlaySound() {
    return this.entity != null && this.entity.isAlive() && !this.entity.isExploding();
  }
}
