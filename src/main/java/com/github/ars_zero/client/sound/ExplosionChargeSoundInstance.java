package com.github.ars_zero.client.sound;

import com.github.ars_zero.common.entity.explosion.ExplosionControllerEntity;
import com.github.ars_zero.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;

public class ExplosionChargeSoundInstance extends AbstractTickableSoundInstance {
    private final ExplosionControllerEntity entity;
    private static final double DEFAULT_CHARGE_TIME_SECONDS = 4.0;

    public ExplosionChargeSoundInstance(ExplosionControllerEntity entity) {
        super(ModSounds.EXPLOSION_CHARGE.get(), SoundSource.NEUTRAL, entity.level().getRandom());
        this.entity = entity;
        this.volume = 3.0f;
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
            
            double chargeTimeSeconds = this.entity.calculateChargeTimeSeconds();
            float chargePitch = (float) (DEFAULT_CHARGE_TIME_SECONDS / chargeTimeSeconds);
            this.pitch = Math.max(0.5f, Math.min(2.0f, chargePitch));
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

