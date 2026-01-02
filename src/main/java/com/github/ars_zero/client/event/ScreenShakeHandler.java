package com.github.ars_zero.client.event;

import com.github.ars_zero.client.ScreenShakeManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = "ars_zero", value = Dist.CLIENT)
public class ScreenShakeHandler {
    
    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        int tickCount = player.tickCount;
        float partialTick = (float) event.getPartialTick();
        
        float shakeY = ScreenShakeManager.getShakeY(tickCount, partialTick);
        float shakeRot = ScreenShakeManager.getShakeRotation(tickCount, partialTick);
        
        event.setYaw(event.getYaw() + shakeRot);
        event.setPitch(event.getPitch() + shakeY * 0.1f);
        event.setRoll(event.getRoll() + shakeRot * 0.5f);
    }
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ScreenShakeManager.tick();
    }
}

