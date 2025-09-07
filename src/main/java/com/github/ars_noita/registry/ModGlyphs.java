package com.github.ars_noita.registry;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.common.glyph.ConjureVoxelEffect;
import com.github.ars_noita.common.glyph.TemporalContextForm;
import com.github.ars_noita.common.glyph.TranslateEffect;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;

public class ModGlyphs {
    
    public static final TemporalContextForm TEMPORAL_CONTEXT_FORM = new TemporalContextForm();
    public static final ConjureVoxelEffect CONJURE_VOXEL_EFFECT = new ConjureVoxelEffect();
    public static final TranslateEffect TRANSLATE_EFFECT = new TranslateEffect();

    public static void registerGlyphs() {
        ArsNoita.LOGGER.debug("Starting glyph registration...");
        
        ArsNoita.LOGGER.debug("Registering TemporalContextForm glyph...");
        GlyphRegistry.registerSpell(TEMPORAL_CONTEXT_FORM);
        ArsNoita.LOGGER.info("Successfully registered TemporalContextForm glyph with ID: {}", TEMPORAL_CONTEXT_FORM.getRegistryName());
        
        ArsNoita.LOGGER.debug("Registering ConjureVoxelEffect glyph...");
        GlyphRegistry.registerSpell(CONJURE_VOXEL_EFFECT);
        ArsNoita.LOGGER.info("Successfully registered ConjureVoxelEffect glyph with ID: {}", CONJURE_VOXEL_EFFECT.getRegistryName());
        
        ArsNoita.LOGGER.debug("Registering TranslateEffect glyph...");
        GlyphRegistry.registerSpell(TRANSLATE_EFFECT);
        ArsNoita.LOGGER.info("Successfully registered TranslateEffect glyph with ID: {}", TRANSLATE_EFFECT.getRegistryName());
        
        ArsNoita.LOGGER.debug("Glyph registration completed");
    }
}
