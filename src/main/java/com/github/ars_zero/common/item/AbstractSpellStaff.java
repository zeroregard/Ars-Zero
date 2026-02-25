package com.github.ars_zero.common.item;

import com.hollingsworth.arsnouveau.api.spell.SpellTier;

/**
 * Base for the four tiered spell staves (Novice, Mage, Archmage, Creative) that use ArsZeroStaffGUI.
 * Distinguished from other staff types (e.g. StaffTelekinesis) which extend AbstractStaff directly
 * or via AbstractStaticSpellStaff.
 */
public abstract class AbstractSpellStaff extends AbstractStaff {

    public AbstractSpellStaff(SpellTier tier) {
        super(tier);
    }
}
