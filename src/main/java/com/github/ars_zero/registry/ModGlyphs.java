package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.glyph.ConjureVoxelEffect;
import com.github.ars_zero.common.glyph.EnlargeEffect;
import com.github.ars_zero.common.glyph.NearForm;
import com.github.ars_zero.common.glyph.PushEffect;
import com.github.ars_zero.common.glyph.SplitAugment;
import com.github.ars_zero.common.glyph.TemporalContextForm;
import com.github.ars_zero.common.glyph.TranslateEffect;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;

public class ModGlyphs {
    
    public static final TemporalContextForm TEMPORAL_CONTEXT_FORM = new TemporalContextForm();
    public static final NearForm NEAR_FORM = new NearForm();
    public static final ConjureVoxelEffect CONJURE_VOXEL_EFFECT = new ConjureVoxelEffect();
    public static final TranslateEffect TRANSLATE_EFFECT = new TranslateEffect();
    public static final PushEffect PUSH_EFFECT = new PushEffect();
    public static final EnlargeEffect ENLARGE_EFFECT = new EnlargeEffect();
    public static final SplitAugment SPLIT_AUGMENT = new SplitAugment();

    public static void registerGlyphs() {
        ArsZero.LOGGER.debug("Starting glyph registration...");
        
        ArsZero.LOGGER.debug("Registering TemporalContextForm glyph...");
        GlyphRegistry.registerSpell(TEMPORAL_CONTEXT_FORM);
        ArsZero.LOGGER.info("Successfully registered TemporalContextForm glyph with ID: {}", TEMPORAL_CONTEXT_FORM.getRegistryName());
        
        ArsZero.LOGGER.debug("Registering NearForm glyph...");
        GlyphRegistry.registerSpell(NEAR_FORM);
        ArsZero.LOGGER.info("Successfully registered NearForm glyph with ID: {}", NEAR_FORM.getRegistryName());
        
        ArsZero.LOGGER.debug("Registering ConjureVoxelEffect glyph...");
        GlyphRegistry.registerSpell(CONJURE_VOXEL_EFFECT);
        ArsZero.LOGGER.info("Successfully registered ConjureVoxelEffect glyph with ID: {}", CONJURE_VOXEL_EFFECT.getRegistryName());
        
        ArsZero.LOGGER.debug("Registering TranslateEffect glyph...");
        GlyphRegistry.registerSpell(TRANSLATE_EFFECT);
        ArsZero.LOGGER.info("Successfully registered TranslateEffect glyph with ID: {}", TRANSLATE_EFFECT.getRegistryName());
        
        ArsZero.LOGGER.debug("Registering PushEffect glyph...");
        GlyphRegistry.registerSpell(PUSH_EFFECT);
        ArsZero.LOGGER.info("Successfully registered PushEffect glyph with ID: {}", PUSH_EFFECT.getRegistryName());
        
        ArsZero.LOGGER.debug("Registering EnlargeEffect glyph...");
        GlyphRegistry.registerSpell(ENLARGE_EFFECT);
        ArsZero.LOGGER.info("Successfully registered EnlargeEffect glyph with ID: {}", ENLARGE_EFFECT.getRegistryName());
        
        ArsZero.LOGGER.debug("Registering SplitAugment glyph...");
        GlyphRegistry.registerSpell(SPLIT_AUGMENT);
        ArsZero.LOGGER.info("Successfully registered SplitAugment glyph with ID: {}", SPLIT_AUGMENT.getRegistryName());
        
        ArsZero.LOGGER.debug("Glyph registration completed");
    }
}
