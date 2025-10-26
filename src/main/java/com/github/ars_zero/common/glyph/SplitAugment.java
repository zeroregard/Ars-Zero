package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class SplitAugment extends AbstractAugment {
    
    public static final String ID = "split";
    public static final SplitAugment INSTANCE = new SplitAugment();

    public SplitAugment() {
        super(ID, "Split");
    }

    @Override
    public int getDefaultManaCost() {
        return 10;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.ONE;
    }

    @NotNull
    @Override
    public Set<SpellSchool> getSchools() {
        return Set.of(SpellSchools.MANIPULATION);
    }

    @Override
    public String getBookDescription() {
        return "Splits the conjured voxel into multiple smaller entities. Each level of Split creates more entities with smaller sizes: 1 split = 3 entities (3x3x3), 2 splits = 5 entities (2x2x2), 3 splits = 7 entities (1x1x1).";
    }

    @Override
    public ResourceLocation getRegistryName() {
        return ArsZero.prefix(ID);
    }
}