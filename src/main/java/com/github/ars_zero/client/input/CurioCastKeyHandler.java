package com.github.ars_zero.client.input;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.registry.ModKeyBindings;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.common.network.PacketCurioCastInput;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ArsZero.MOD_ID, value = Dist.CLIENT)
public class CurioCastKeyHandler {
    private static boolean wasPressed = false;
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        boolean isPressed = ModKeyBindings.CURIO_CAST.isDown();
        if (isPressed != wasPressed) {
            wasPressed = isPressed;
            Networking.sendToServer(new PacketCurioCastInput(isPressed));
        }
    }
}
