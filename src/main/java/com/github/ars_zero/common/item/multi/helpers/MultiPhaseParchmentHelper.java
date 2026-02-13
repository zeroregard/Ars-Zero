package com.github.ars_zero.common.item.multi.helpers;

import com.github.ars_zero.common.casting.CastingStyle;
import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.spell.StaffSpellClipboard;
import com.github.ars_zero.common.spell.SpellPhase;
import com.github.ars_zero.registry.ModItems;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Helpers for creating multiphase spell parchments from device stacks (e.g. at the Scribes table).
 */
public final class MultiPhaseParchmentHelper {

    private static final int SLOT_COUNT = 10;

    private MultiPhaseParchmentHelper() {
    }

    /**
     * Creates a multiphase spell parchment with the first non-empty slot from the given device stack.
     * Used when inscribing from a device (on table or in hand) onto the Scribes table.
     */
    public static Optional<ItemStack> createMultiphaseParchmentFromDevice(ItemStack deviceStack) {
        if (deviceStack == null || deviceStack.isEmpty()) {
            return Optional.empty();
        }
        AbstractCaster<?> caster = SpellCasterRegistry.from(deviceStack);
        if (caster == null) {
            return Optional.empty();
        }
        int logicalSlot = -1;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int beginPhysical = slot * 3 + SpellPhase.BEGIN.ordinal();
            int tickPhysical = slot * 3 + SpellPhase.TICK.ordinal();
            int endPhysical = slot * 3 + SpellPhase.END.ordinal();
            if (!caster.getSpell(beginPhysical).isEmpty() || !caster.getSpell(tickPhysical).isEmpty() || !caster.getSpell(endPhysical).isEmpty()) {
                logicalSlot = slot;
                break;
            }
        }
        if (logicalSlot < 0) {
            return Optional.empty();
        }
        int beginPhysical = logicalSlot * 3 + SpellPhase.BEGIN.ordinal();
        int tickPhysical = logicalSlot * 3 + SpellPhase.TICK.ordinal();
        int endPhysical = logicalSlot * 3 + SpellPhase.END.ordinal();
        Spell beginSpell = caster.getSpell(beginPhysical);
        Spell tickSpell = caster.getSpell(tickPhysical);
        Spell endSpell = caster.getSpell(endPhysical);
        String name = caster.getSpellName(beginPhysical);
        if (name == null || name.isEmpty()) {
            name = caster.getSpellName(tickPhysical);
        }
        if (name == null || name.isEmpty()) {
            name = caster.getSpellName(endPhysical);
        }
        if (name == null) {
            name = "";
        }
        int delay = AbstractMultiPhaseCastDevice.getSlotTickDelay(deviceStack, logicalSlot);
        CastingStyle castingStyle = AbstractMultiPhaseCastDevice.getCastingStyle(deviceStack, logicalSlot);
        StaffSpellClipboard clipboard = new StaffSpellClipboard(
            beginSpell == null || beginSpell.isEmpty() ? new Spell() : beginSpell,
            tickSpell == null || tickSpell.isEmpty() ? new Spell() : tickSpell,
            endSpell == null || endSpell.isEmpty() ? new Spell() : endSpell,
            name,
            delay,
            castingStyle == null ? new CastingStyle() : castingStyle
        );
        ItemStack multiphase = new ItemStack(ModItems.MULTIPHASE_SPELL_PARCHMENT.get(), 1);
        StaffSpellClipboard.writeToStack(multiphase, clipboard, StaffSpellClipboard.PARCHMENT_SLOT_KEY);
        return Optional.of(multiphase);
    }
}
