package com.github.ars_zero.common.glyph.augment;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.shape.BaseShape;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;

public class AugmentSphere extends AbstractAugment implements IShapeAugment {
    public static final String ID = "augment_sphere";
    public static final AugmentSphere INSTANCE = new AugmentSphere();

    private AugmentSphere() {
        super(ArsZero.prefix(ID), "Sphere");
    }

    @Override
    public BaseShape getShape() {
        return BaseShape.SPHERE;
    }

    @Override
    public int getDefaultManaCost() {
        return 15;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @Override
    public String getBookDescription() {
        return "Used with Geometrize to generate spherical shapes. When flattened, produces circles.";
    }
}

