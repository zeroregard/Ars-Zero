package com.github.ars_zero.client.input;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.RadialMenuTracker;
import com.github.ars_zero.client.gui.SpellcastingCircletGUI;
import com.github.ars_zero.client.registry.ModKeyBindings;
import com.github.ars_zero.common.item.SpellcastingCirclet;
import com.github.ars_zero.common.network.CircletSlotInfo;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.common.network.PacketCurioCastInput;
import com.github.ars_zero.common.network.PacketPutCircletBack;
import com.github.ars_zero.common.network.PacketSwapCircletToHand;
import com.hollingsworth.arsnouveau.api.util.StackUtil;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.GuiRadialMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import static com.hollingsworth.arsnouveau.client.registry.ModKeyBindings.OPEN_BOOK;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = ArsZero.MOD_ID, value = Dist.CLIENT)
public final class CurioCastKeyHandler {

    private static boolean wasCastingPressed = false;
    /** Set when server sent open-circlet-gui; we open next tick after inventory sync. */
    private static CircletSlotInfo pendingOpenCircletGui;
    /** Set when circlet GUI (with put-back slot) closes; we put back when all screens close (screen == null). */
    private static CircletSlotInfo pendingPutCircletBack;

    private CurioCastKeyHandler() {
    }

    public static void setPendingOpenCircletGui(CircletSlotInfo slot) {
        pendingOpenCircletGui = slot;
    }

    public static void setPendingPutCircletBack(CircletSlotInfo slot) {
        pendingPutCircletBack = slot;
    }

    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        if (event.getAction() != 1) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        // C = spellbook GUI key in Ars Nouveau; open circlet GUI when no book/staff in hand but circlet equipped.
        // Only when main hand is empty: request server to swap circlet to hand, then server sends open-gui and we set "opened via swap".
        if (event.getKey() != OPEN_BOOK.getKey().getValue() || OPEN_BOOK.isUnbound()) {
            return;
        }
        if (StackUtil.getHeldCasterTool(minecraft.player) != null) {
            return;
        }
        if (!minecraft.player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            return;
        }
        CuriosApi.getCuriosInventory(minecraft.player)
                .flatMap(handler -> handler.findCurios(stack -> stack.getItem() instanceof SpellcastingCirclet).stream().findFirst())
                .ifPresent(slotResult -> Networking.sendToServer(new PacketSwapCircletToHand()));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (pendingOpenCircletGui != null && minecraft.screen == null) {
            ItemStack mainHand = minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
            if (mainHand.getItem() instanceof SpellcastingCirclet) {
                minecraft.setScreen(new SpellcastingCircletGUI(mainHand, InteractionHand.MAIN_HAND, pendingOpenCircletGui));
                pendingOpenCircletGui = null;
            }
        }

        if (minecraft.screen == null && pendingPutCircletBack != null) {
            Networking.sendToServer(new PacketPutCircletBack(pendingPutCircletBack));
            pendingPutCircletBack = null;
        }

        boolean pressed = ModKeyBindings.CURIO_CAST.isDown();
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

    @SubscribeEvent
    public static void onCircletGuiClosed(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof SpellcastingCircletGUI gui) {
            gui.getPutBackSlot().ifPresent(CurioCastKeyHandler::setPendingPutCircletBack);
        }
    }
}

