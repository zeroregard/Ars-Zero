package com.github.ars_zero.client.input;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.registry.ModKeyBindings;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.item.SpellcastingCirclet;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.common.network.PacketCurioCastInput;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

@EventBusSubscriber(modid = ArsZero.MOD_ID, value = Dist.CLIENT)
public class CurioCastKeyHandler {
    private static boolean wasPressed = false;
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (ModKeyBindings.CURIO_RADIAL.consumeClick()) {
            openRadial(minecraft.player);
        }
        boolean isPressed = ModKeyBindings.CURIO_CAST.isDown();
        if (isPressed != wasPressed) {
            wasPressed = isPressed;
            Networking.sendToServer(new PacketCurioCastInput(isPressed));
        }
    }
    
    private static void openRadial(Player player) {
        findCirclet(player).ifPresent(stack -> {
            if (stack.getItem() instanceof AbstractSpellStaff staff) {
                staff.onRadialKeyPressed(stack, player);
            }
        });
    }
    
    private static Optional<ItemStack> findCirclet(Player player) {
        return CuriosApi.getCuriosHelper().findEquippedCurio(
            stack -> stack.getItem() instanceof SpellcastingCirclet,
            player
        ).map(result -> result.getRight());
    }
}
