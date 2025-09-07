package com.github.ars_noita.registry;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.common.glyph.TemporalContextForm;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;

public class ModGlyphs {
    
    public static final TemporalContextForm TEMPORAL_CONTEXT_FORM = new TemporalContextForm();

    public static void registerGlyphs() {
        GlyphRegistry.registerSpell(TEMPORAL_CONTEXT_FORM);
    }
}
