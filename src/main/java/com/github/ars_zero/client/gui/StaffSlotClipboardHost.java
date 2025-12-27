package com.github.ars_zero.client.gui;

import com.github.ars_zero.client.gui.spell.SpellPhaseSlots;
import com.github.ars_zero.common.spell.SpellPhase;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public interface StaffSlotClipboardHost {

    AbstractCaster<?> getHostCaster();

    ItemStack getHostDeviceStack();

    InteractionHand getHostGuiHand();

    boolean isHostCircletDevice();

    int getHostSelectedSpellSlot();

    int getHostStoredDelayValueForSlot(int logicalSlot);

    void setHostStoredDelayValueForSlot(int logicalSlot, int delay);

    void setHostSlotSpellName(int logicalSlot, String name);

    void setHostSpellNameBoxValue(String value);

    SpellPhaseSlots getHostPhaseSpells();

    void hostResetCraftingCells();

    void hostValidate();

    SpellPhase getHostCurrentPhase();
}


