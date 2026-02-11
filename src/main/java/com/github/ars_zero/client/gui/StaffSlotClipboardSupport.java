package com.github.ars_zero.client.gui;

import com.github.ars_zero.client.gui.buttons.StaffSpellSlot;
import com.github.ars_zero.common.casting.CastingStyle;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.network.ArsNouveauNetworking;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.common.network.PacketConvertParchmentToMultiphase;
import com.github.ars_zero.common.network.PacketSetParchmentClipboard;
import com.github.ars_zero.common.network.PacketUpdateCastingStyle;
import com.github.ars_zero.common.network.PacketUpdateTickDelay;
import com.github.ars_zero.common.spell.SpellPhase;
import com.github.ars_zero.common.spell.StaffSpellClipboard;
import com.github.ars_zero.registry.ModItems;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.network.PacketUpdateCaster;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class StaffSlotClipboardSupport {

    private final StaffSlotClipboardHost host;
    private final StaffSlotContextMenu menu;

    public StaffSlotClipboardSupport(StaffSlotClipboardHost host) {
        this.host = Objects.requireNonNull(host);
        this.menu = new StaffSlotContextMenu(Minecraft.getInstance().font);
    }

    public void onInit() {
        menu.hide();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (menu.isVisible()) {
            menu.render(graphics, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, StaffSpellSlot[] spellSlots) {
        if (menu.isVisible()) {
            if (menu.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            menu.hide();
        }

        if (button != 1) {
            return false;
        }

        for (StaffSpellSlot slot : spellSlots) {
            if (slot != null && slot.isMouseOver(mouseX, mouseY)) {
                int logicalSlot = slot.slotNum;
                boolean pasteEnabled = hasPasteSource();
                menu.show((int) mouseX, (int) mouseY, pasteEnabled, () -> copyWholeSlot(logicalSlot), () -> pasteWholeSlot(logicalSlot));
                return true;
            }
        }

        return false;
    }

    private void copyWholeSlot(int logicalSlot) {
        var caster = host.getHostCaster();
        if (caster == null) {
            return;
        }

        int beginPhysicalSlot = logicalSlot * 3 + SpellPhase.BEGIN.ordinal();
        int tickPhysicalSlot = logicalSlot * 3 + SpellPhase.TICK.ordinal();
        int endPhysicalSlot = logicalSlot * 3 + SpellPhase.END.ordinal();

        Spell beginSpell = caster.getSpell(beginPhysicalSlot);
        Spell tickSpell = caster.getSpell(tickPhysicalSlot);
        Spell endSpell = caster.getSpell(endPhysicalSlot);
        String name = caster.getSpellName(beginPhysicalSlot);
        int delay = host.getHostStoredDelayValueForSlot(logicalSlot);
        CastingStyle castingStyle = AbstractMultiPhaseCastDevice.getCastingStyle(host.getHostDeviceStack(), logicalSlot);

        StaffSpellClipboard clipboard = new StaffSpellClipboard(
            beginSpell == null ? new Spell() : beginSpell,
            tickSpell == null ? new Spell() : tickSpell,
            endSpell == null ? new Spell() : endSpell,
            name,
            delay,
            castingStyle
        );

        StaffSpellClipboardClient.set(clipboard);

        // If other hand holds a parchment: multiphase → write data; regular spell parchment → convert to multiphase
        ItemStack otherHandStack = getOtherHandStack();
        if (!otherHandStack.isEmpty()) {
            InteractionHand guiHand = host.getHostGuiHand();
            boolean parchmentInMainHand = (guiHand != null && guiHand == InteractionHand.OFF_HAND);
            if (otherHandStack.getItem() == ModItems.MULTIPHASE_SPELL_PARCHMENT.get()) {
                StaffSpellClipboard.writeToStack(otherHandStack, clipboard, StaffSpellClipboard.PARCHMENT_SLOT_KEY);
                Networking.sendToServer(new PacketSetParchmentClipboard(clipboard.toTag(), parchmentInMainHand));
            } else if (PacketConvertParchmentToMultiphase.isSpellParchment(otherHandStack)) {
                Networking.sendToServer(new PacketConvertParchmentToMultiphase(clipboard.toTag(), parchmentInMainHand));
            }
        }
    }

    private void pasteWholeSlot(int logicalSlot) {
        StaffSpellClipboard clipboard = resolvePasteClipboard();
        if (clipboard == null) {
            return;
        }

        int beginPhysicalSlot = logicalSlot * 3 + SpellPhase.BEGIN.ordinal();
        int tickPhysicalSlot = logicalSlot * 3 + SpellPhase.TICK.ordinal();
        int endPhysicalSlot = logicalSlot * 3 + SpellPhase.END.ordinal();

        ArsNouveauNetworking.sendToServer(new PacketUpdateCaster(clipboard.begin(), beginPhysicalSlot, clipboard.name(), true));
        ArsNouveauNetworking.sendToServer(new PacketUpdateCaster(clipboard.tick(), tickPhysicalSlot, clipboard.name(), true));
        ArsNouveauNetworking.sendToServer(new PacketUpdateCaster(clipboard.end(), endPhysicalSlot, clipboard.name(), true));

        applyDelayToSlot(logicalSlot, clipboard.tickDelay());
        host.setHostSlotSpellName(logicalSlot, clipboard.name());

        if (clipboard.castingStyle() != null) {
            Networking.sendToServer(new PacketUpdateCastingStyle(logicalSlot, clipboard.castingStyle(), host.isHostCircletDevice()));
        }

        if (logicalSlot == host.getHostSelectedSpellSlot()) {
            host.setHostSpellNameBoxValue(clipboard.name());
            host.getHostPhaseSpells().getPhaseList(SpellPhase.BEGIN).clear();
            host.getHostPhaseSpells().getPhaseList(SpellPhase.TICK).clear();
            host.getHostPhaseSpells().getPhaseList(SpellPhase.END).clear();
            fillPhaseListFromSpell(SpellPhase.BEGIN, clipboard.begin());
            fillPhaseListFromSpell(SpellPhase.TICK, clipboard.tick());
            fillPhaseListFromSpell(SpellPhase.END, clipboard.end());
            host.hostResetCraftingCells();
            host.hostValidate();
        }
    }

    private void applyDelayToSlot(int logicalSlot, int delay) {
        int clamped = Mth.clamp(delay, 1, 20);
        host.setHostStoredDelayValueForSlot(logicalSlot, clamped);

        InteractionHand guiHand = host.getHostGuiHand();
        boolean mainHand = guiHand == null || guiHand == InteractionHand.MAIN_HAND;
        Networking.sendToServer(new PacketUpdateTickDelay(logicalSlot, clamped, mainHand, host.isHostCircletDevice()));
    }

    private void fillPhaseListFromSpell(SpellPhase phase, Spell spell) {
        List<AbstractSpellPart> phaseList = host.getHostPhaseSpells().getPhaseList(phase);
        for (int i = 0; i < 10; i++) {
            phaseList.add(null);
        }
        if (spell == null || spell.isEmpty()) {
            return;
        }
        List<AbstractSpellPart> recipeList = new ArrayList<>();
        for (AbstractSpellPart part : spell.recipe()) {
            recipeList.add(part);
        }
        for (int i = 0; i < recipeList.size() && i < 10; i++) {
            phaseList.set(i, recipeList.get(i));
        }
    }

    /** Paste source order: in-memory client clipboard first, then other-hand parchment. */
    private StaffSpellClipboard resolvePasteClipboard() {
        return StaffSpellClipboardClient.get()
            .or(this::getParchmentClipboardFromOtherHand)
            .orElse(null);
    }

    private boolean hasPasteSource() {
        return StaffSpellClipboardClient.get().isPresent()
            || getParchmentClipboardFromOtherHand().isPresent();
    }

    private ItemStack getOtherHandStack() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        InteractionHand guiHand = host.getHostGuiHand();
        InteractionHand other = (guiHand == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return player.getItemInHand(other);
    }

    private Optional<StaffSpellClipboard> getParchmentClipboardFromOtherHand() {
        ItemStack other = getOtherHandStack();
        if (other.isEmpty() || other.getItem() != ModItems.MULTIPHASE_SPELL_PARCHMENT.get()) {
            return Optional.empty();
        }
        return StaffSpellClipboard.readFromStack(other, StaffSpellClipboard.PARCHMENT_SLOT_KEY);
    }

    /** Called from screen for Ctrl+C: copy the given slot (in-memory, device, and parchment in other hand if present). */
    public void copySlot(int logicalSlot) {
        copyWholeSlot(logicalSlot);
    }

    /** Called from screen for Ctrl+V: paste into the given slot from clipboard (client, device, or parchment). */
    public void pasteSlot(int logicalSlot) {
        pasteWholeSlot(logicalSlot);
    }
}


