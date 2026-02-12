package com.github.ars_zero.common.glyph.augment;

import com.github.ars_zero.ArsZero;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import net.neoforged.neoforge.common.ModConfigSpec;

public class AugmentAOEThree extends AbstractAugment {

    public static final String ID = "augment_aoe_three";
    public static final AugmentAOEThree INSTANCE = new AugmentAOEThree();

    private AugmentAOEThree() {
        super(ArsZero.prefix(ID), "AOE III");
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("General settings").push("general");
        ENABLED = builder.comment("Is Enabled? (Optional glyph from Adams Ars Plus, disabled by default).")
                .define("enabled", false);
        COST = builder.comment("Cost").defineInRange("cost", getDefaultManaCost(), Integer.MIN_VALUE, Integer.MAX_VALUE);
        STARTER_SPELL = builder.comment("Is Starter Glyph?").define("starter", defaultedStarterGlyph());
        PER_SPELL_LIMIT = builder.comment("The maximum number of times this glyph may appear in a single spell")
                .defineInRange("per_spell_limit", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        GLYPH_TIER = builder.comment("The tier of the glyph").defineInRange("glyph_tier", defaultTier().value, 1, 99);
        builder.pop();
    }

    @Override
    public int getDefaultManaCost() {
        return AugmentAOE.INSTANCE.getDefaultManaCost() * 9;
    }

    @Override
    public String getBookDescription() {
        return "Spells will affect a gargantuan area around a targeted block.";
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.THREE;
    }

    @Override
    public SpellStats.Builder applyModifiers(SpellStats.Builder builder, AbstractSpellPart spellPart) {
        builder.addAOE(4.0);
        return super.applyModifiers(builder, spellPart);
    }
}
