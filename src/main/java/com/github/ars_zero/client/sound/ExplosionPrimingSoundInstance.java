package com.github.ars_zero.client.sound;

import com.github.ars_zero.common.entity.explosion.ExplosionControllerEntity;
import com.github.ars_zero.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;

public class ExplosionPrimingSoundInstance extends AbstractTickableSoundInstance {
    private final ExplosionControllerEntity entity;

    public ExplosionPrimingSoundInstance(ExplosionControllerEntity entity) {
        super(ModSounds.EXPLOSION_PRIMING.get(), SoundSource.NEUTRAL, entity.level().getRandom());
        this.entity = entity;
        this.volume = 5.0f;
        this.pitch = 1.0f;
        this.looping = false;
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

