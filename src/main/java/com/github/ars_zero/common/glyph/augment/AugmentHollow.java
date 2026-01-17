package com.github.ars_zero.common.glyph.augment;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.shape.FillMode;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;

public class AugmentHollow extends AbstractAugment implements IFillAugment {
    public static final String ID = "augment_hollow";
    public static final AugmentHollow INSTANCE = new AugmentHollow();

    private AugmentHollow() {
        super(ArsZero.prefix(ID), "Hollow");
    }

    @Override
    public FillMode getFillMode() {
        return FillMode.HOLLOW;
    }

    @Override
    public int getDefaultManaCost() {
        return 10;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.THREE;
    }

    @Override
    public String getBookDescription() {
        return "Used with Geometrize to generate hollow shapes, only placing the outer shell.";
    }
}
