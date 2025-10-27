package com.github.ars_zero.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class StaffLoopingSound extends AbstractTickableSoundInstance {
    
    private final Player player;
    private boolean shouldStop = false;
    
    public StaffLoopingSound(Player player) {
        super(SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.2f;
        this.pitch = 1.2f;
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
    }
    
    @Override
    public void tick() {
        if (shouldStop || !player.isAlive() || player.isRemoved()) {
            this.stop();
            return;
        }
        
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
    }
    
    public void fadeOut() {
        this.shouldStop = true;
    }
    
    @Override
    public boolean canStartSilent() {
        return true;
    }
}

