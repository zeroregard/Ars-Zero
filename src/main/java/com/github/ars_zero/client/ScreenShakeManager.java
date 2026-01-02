package com.github.ars_zero.client;

import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScreenShakeManager {
    private static float currentShakeIntensity = 0.0f;
    private static int shakeDurationRemaining = 0;
    private static int totalShakeDuration = 0;
    
    public static void addShake(float intensity, int durationTicks) {
        if (intensity > currentShakeIntensity || shakeDurationRemaining <= 0) {
            currentShakeIntensity = intensity;
            shakeDurationRemaining = durationTicks;
            totalShakeDuration = durationTicks;
        } else {
            currentShakeIntensity = Math.max(currentShakeIntensity, intensity * 0.5f);
            if (durationTicks > shakeDurationRemaining) {
                shakeDurationRemaining = durationTicks;
                totalShakeDuration = durationTicks;
            }
        }
    }
    
    public static void tick() {
        if (shakeDurationRemaining > 0) {
            shakeDurationRemaining--;
            
            if (shakeDurationRemaining <= 0) {
                currentShakeIntensity = 0.0f;
                totalShakeDuration = 0;
            }
        }
    }
    
    public static float getShakeX(int tickCount, float partialTick) {
        if (currentShakeIntensity <= 0.0f || shakeDurationRemaining <= 0) {
            return 0.0f;
        }
        
        float progress = totalShakeDuration > 0 
            ? 1.0f - ((float) shakeDurationRemaining / (float) totalShakeDuration)
            : 0.0f;
        float decayFactor = 1.0f - progress;
        float intensity = currentShakeIntensity * decayFactor;
        
        float time = (tickCount + partialTick) * 0.15f;
        return Mth.sin(time * 50.0f) * intensity * 0.5f;
    }
    
    public static float getShakeY(int tickCount, float partialTick) {
        if (currentShakeIntensity <= 0.0f || shakeDurationRemaining <= 0) {
            return 0.0f;
        }
        
        float progress = totalShakeDuration > 0 
            ? 1.0f - ((float) shakeDurationRemaining / (float) totalShakeDuration)
            : 0.0f;
        float decayFactor = 1.0f - progress;
        float intensity = currentShakeIntensity * decayFactor;
        
        float time = (tickCount + partialTick) * 0.15f;
        return Mth.cos(time * 47.0f) * intensity * 0.5f;
    }
    
    public static float getShakeRotation(int tickCount, float partialTick) {
        if (currentShakeIntensity <= 0.0f || shakeDurationRemaining <= 0) {
            return 0.0f;
        }
        
        float progress = totalShakeDuration > 0 
            ? 1.0f - ((float) shakeDurationRemaining / (float) totalShakeDuration)
            : 0.0f;
        float decayFactor = 1.0f - progress;
        float intensity = currentShakeIntensity * decayFactor;
        
        float time = (tickCount + partialTick) * 0.15f;
        return Mth.sin(time * 43.0f) * intensity * 2.0f;
    }
}

