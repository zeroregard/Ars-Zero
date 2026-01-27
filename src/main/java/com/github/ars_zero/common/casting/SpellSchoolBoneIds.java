package com.github.ars_zero.common.casting;

import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class SpellSchoolBoneIds {
    private static final String FALLBACK_BONE = "school_manipulation";
    private static final Map<String, String> SCHOOL_ID_TO_BONE = Map.ofEntries(
        Map.entry("abjuration", "school_abjuration"),
        Map.entry("conjuration", "school_conjuration"),
        Map.entry("necromancy", "school_anima"),
        Map.entry("manipulation", "school_manipulation"),
        Map.entry("air", "school_air"),
        Map.entry("earth", "school_earth"),
        Map.entry("fire", "school_fire"),
        Map.entry("water", "school_water")
    );

    private SpellSchoolBoneIds() {
    }

    @Nullable
    public static String firstSchoolBoneIdFromSpell(Spell spell) {
        if (spell == null || spell.isEmpty()) {
            return null;
        }
        for (AbstractSpellPart part : spell.recipe()) {
            if (!(part instanceof AbstractEffect)) {
                continue;
            }
            if (part.spellSchools == null || part.spellSchools.isEmpty()) {
                continue;
            }
            String schoolId = part.spellSchools.get(0).getId();
            String bone = SCHOOL_ID_TO_BONE.get(schoolId);
            return bone != null ? bone : FALLBACK_BONE;
        }
        return FALLBACK_BONE;
    }
}
