package com.github.ars_zero.client.sound;

import com.github.ars_zero.common.entity.explosion.ExplosionControllerEntity;
import com.github.ars_zero.registry.ModSounds;

public class ExplosionPrimingSoundInstance extends AbstractExplosionSoundInstance {
    private final ExplosionControllerEntity entity;

    public ExplosionPrimingSoundInstance(ExplosionControllerEntity entity) {
        super(entity.getX(), entity.getY(), entity.getZ(), ModSounds.EXPLOSION_PRIMING.get(), entity.level().getRandom());
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
        }

        updateVolumeFromDistance();
    }

    @Override
    public boolean canPlaySound() {
        return this.entity != null && this.entity.isAlive() && !this.entity.isExploding();
    }
}

