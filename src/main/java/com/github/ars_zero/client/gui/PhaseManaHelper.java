package com.github.ars_zero.client.gui;

import com.hollingsworth.arsnouveau.api.mana.IManaDiscountEquipment;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable helper for phase spell mana costs, with optional discount from the casting device.
 */
public final class PhaseManaHelper {

    private PhaseManaHelper() {
    }

    /**
     * Raw mana cost of the phase spell (no discount).
     */
    public static int getRawCost(List<AbstractSpellPart> phaseSpell) {
        Spell spell = spellFromParts(phaseSpell);
        return spell.getCost();
    }

    /**
     * Display cost for a phase: raw cost minus discount if the device implements {@link IManaDiscountEquipment}.
     * Pass {@link ItemStack#EMPTY} for no discount.
     */
    public static int getDisplayCost(List<AbstractSpellPart> phaseSpell, ItemStack deviceStack) {
        Spell spell = spellFromParts(phaseSpell);
        int cost = spell.getCost();
        if (deviceStack.isEmpty()) {
            return cost;
        }
        if (deviceStack.getItem() instanceof IManaDiscountEquipment discountEquipment) {
            int discount = discountEquipment.getManaDiscount(deviceStack, spell);
            return Math.max(0, cost - discount);
        }
        return cost;
    }

    private static Spell spellFromParts(List<AbstractSpellPart> phaseSpell) {
        List<AbstractSpellPart> filtered = new ArrayList<>();
        for (AbstractSpellPart part : phaseSpell) {
            if (part != null) {
                filtered.add(part);
            }
        }
        return new Spell(filtered);
    }
}
