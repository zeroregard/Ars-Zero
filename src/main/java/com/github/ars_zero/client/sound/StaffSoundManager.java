package com.github.ars_zero.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class StaffSoundManager {
    
    private static StaffLoopingSound currentLoopingSound = null;
    
    public static void startLoopingSound(Player player) {
        stopLoopingSound();
        
        currentLoopingSound = new StaffLoopingSound(player);
        Minecraft.getInstance().getSoundManager().play(currentLoopingSound);
    }
    
    public static void stopLoopingSound() {
        if (currentLoopingSound != null) {
            currentLoopingSound.fadeOut();
            currentLoopingSound = null;
        }
    }
}

