package com.github.ars_noita.registry;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.common.glyph.TemporalContextForm;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;

public class ModGlyphs {
    
    public static final TemporalContextForm TEMPORAL_CONTEXT_FORM = new TemporalContextForm();

    public static void registerGlyphs() {
        ArsNoita.LOGGER.debug("Starting glyph registration...");
        
        ArsNoita.LOGGER.debug("Registering TemporalContextForm glyph...");
        GlyphRegistry.registerSpell(TEMPORAL_CONTEXT_FORM);
        ArsNoita.LOGGER.info("Successfully registered TemporalContextForm glyph with ID: {}", TEMPORAL_CONTEXT_FORM.getRegistryName());
        
        ArsNoita.LOGGER.debug("Glyph registration completed");
    }
}
