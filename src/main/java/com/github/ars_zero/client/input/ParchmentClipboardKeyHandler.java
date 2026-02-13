package com.github.ars_zero.client.input;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.gui.AbstractMultiPhaseCastDeviceScreen;
import com.github.ars_zero.client.gui.StaffSpellClipboardClient;
import com.github.ars_zero.common.spell.StaffSpellClipboard;
import com.github.ars_zero.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

/**
 * When the player holds a multiphase spell parchment and presses Ctrl+C (or Cmd+C on Mac)
 * outside the staff GUI, copies the parchment's slot into the in-memory clipboard so they
 * can open a staff/circlet and Ctrl+V to paste it into a slot.
 */
@EventBusSubscriber(modid = ArsZero.MOD_ID, value = Dist.CLIENT)
public final class ParchmentClipboardKeyHandler {

    private ParchmentClipboardKeyHandler() {
    }

    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        if (event.getKey() != GLFW.GLFW_KEY_C) {
            return;
        }
        int mods = event.getModifiers();
        boolean copyModifier = (mods & GLFW.GLFW_MOD_CONTROL) != 0 || (mods & GLFW.GLFW_MOD_SUPER) != 0;
        if (!copyModifier) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        // Don't override copy when the staff GUI is open (it copies from the selected slot)
        if (mc.screen instanceof AbstractMultiPhaseCastDeviceScreen) {
            return;
        }

        ItemStack main = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off = mc.player.getItemInHand(InteractionHand.OFF_HAND);
        ItemStack parchment = ItemStack.EMPTY;
        if (main.getItem() == ModItems.MULTIPHASE_SPELL_PARCHMENT.get()) {
            parchment = main;
        } else if (off.getItem() == ModItems.MULTIPHASE_SPELL_PARCHMENT.get()) {
            parchment = off;
        }
        if (parchment.isEmpty()) {
            return;
        }

        Optional<StaffSpellClipboard> clip = StaffSpellClipboard.readFromStack(parchment, StaffSpellClipboard.PARCHMENT_SLOT_KEY);
        clip.ifPresent(StaffSpellClipboardClient::set);
    }
}
