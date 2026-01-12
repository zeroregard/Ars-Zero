package com.github.ars_zero.common.glyph.augment;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.shape.ProjectionMode;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;

public class AugmentFlatten extends AbstractAugment implements IProjectionAugment {
    public static final String ID = "augment_flatten";
    public static final AugmentFlatten INSTANCE = new AugmentFlatten();

    private AugmentFlatten() {
        super(ArsZero.prefix(ID), "Flatten");
    }

    @Override
    public ProjectionMode getProjectionMode() {
        return ProjectionMode.FLATTENED;
    }

    @Override
    public int getDefaultManaCost() {
        return 5;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @Override
    public String getBookDescription() {
        return "Projects a 3D shape into 2D based on the caster's look direction at resolve time. Spheres become circles, cubes become squares.";
    }
}

