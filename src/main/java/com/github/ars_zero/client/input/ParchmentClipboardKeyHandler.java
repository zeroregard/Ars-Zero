package com.github.ars_zero.client.input;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.gui.AbstractMultiPhaseCastDeviceScreen;
import com.github.ars_zero.client.gui.StaffSpellClipboardClient;
import com.github.ars_zero.common.casting.CastingStyle;
import com.github.ars_zero.common.item.AbstractStaticSpellStaff;
import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.spell.StaffSpellClipboard;
import com.github.ars_zero.registry.ModItems;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
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
 * When the player holds a multiphase spell parchment or a static staff and presses Ctrl+C (or Cmd+C on Mac)
 * outside the staff GUI, copies the spells into the in-memory clipboard so they can open a staff/circlet and Ctrl+V to paste.
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

        if (main.getItem() == ModItems.MULTIPHASE_SPELL_PARCHMENT.get() || off.getItem() == ModItems.MULTIPHASE_SPELL_PARCHMENT.get()) {
            ItemStack parchment = main.getItem() == ModItems.MULTIPHASE_SPELL_PARCHMENT.get() ? main : off;
            Optional<StaffSpellClipboard> clip = StaffSpellClipboard.readFromStack(parchment, StaffSpellClipboard.PARCHMENT_SLOT_KEY);
            clip.ifPresent(StaffSpellClipboardClient::set);
            return;
        }

        if (main.getItem() instanceof AbstractStaticSpellStaff || off.getItem() instanceof AbstractStaticSpellStaff) {
            ItemStack staticStaff = main.getItem() instanceof AbstractStaticSpellStaff ? main : off;
            StaffSpellClipboard clip = createClipboardFromStaticStaff(staticStaff);
            if (clip != null) {
                StaffSpellClipboardClient.set(clip);
            }
        }
    }

    private static StaffSpellClipboard createClipboardFromStaticStaff(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AbstractStaticSpellStaff)) {
            return null;
        }
        AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
        if (caster == null) {
            return null;
        }
        Spell begin = caster.getSpell(0);
        Spell tick = caster.getSpell(1);
        Spell end = caster.getSpell(2);
        String name = caster.getSpellName(0);
        int delay = AbstractMultiPhaseCastDevice.getSlotTickDelay(stack, 0);
        CastingStyle style = AbstractMultiPhaseCastDevice.getCastingStyle(stack, 0);
        return new StaffSpellClipboard(
            begin != null ? begin : new Spell(),
            tick != null ? tick : new Spell(),
            end != null ? end : new Spell(),
            name != null ? name : "",
            delay,
            style != null ? style : new CastingStyle()
        );
    }
}
