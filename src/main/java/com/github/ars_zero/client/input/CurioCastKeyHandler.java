package com.github.ars_zero.client.input;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.RadialMenuTracker;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.common.network.PacketCurioCastInput;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.GuiRadialMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = ArsZero.MOD_ID, value = Dist.CLIENT)
public final class CurioCastKeyHandler {

    private static boolean wasCastingPressed = false;

    private CurioCastKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        boolean pressed = com.github.ars_zero.client.registry.ModKeyBindings.CURIO_CAST.isDown();
        if (pressed != wasCastingPressed) {
            wasCastingPressed = pressed;
            Networking.sendToServer(new PacketCurioCastInput(pressed));
        }
    }

    @SubscribeEvent
    public static void onRadialScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof GuiRadialMenu) || !RadialMenuTracker.isCircletActive()) {
            return;
        }

        ItemStack iconStack = RadialMenuTracker.getActiveStack();
        if (iconStack.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);

        int centerX = event.getScreen().width / 2;
        int centerY = event.getScreen().height / 2;
        graphics.renderItem(iconStack, centerX - 8, centerY - 28);
        graphics.renderItemDecorations(mc.font, iconStack, centerX - 8, centerY - 28);

        graphics.pose().popPose();
    }

    @SubscribeEvent
    public static void onRadialScreenClosed(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof GuiRadialMenu) {
            RadialMenuTracker.clear();
        }
    }
}

