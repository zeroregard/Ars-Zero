package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.glyph.ConjureVoxelEffect;
import com.github.ars_zero.common.glyph.NearForm;
import com.github.ars_zero.common.glyph.PushEffect;
import com.github.ars_zero.common.glyph.SelectEffect;
import com.github.ars_zero.common.glyph.TemporalContextForm;
import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.common.glyph.ZeroGravityEffect;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;

public class ModGlyphs {
    
    public static final TemporalContextForm TEMPORAL_CONTEXT_FORM = new TemporalContextForm();
    public static final NearForm NEAR_FORM = new NearForm();
    public static final ConjureVoxelEffect CONJURE_VOXEL_EFFECT = new ConjureVoxelEffect();
    public static final SelectEffect SELECT_EFFECT = new SelectEffect();
    public static final AnchorEffect ANCHOR_EFFECT = new AnchorEffect();
    public static final PushEffect PUSH_EFFECT = new PushEffect();
    public static final ZeroGravityEffect ZERO_GRAVITY_EFFECT = new ZeroGravityEffect();

    public static void registerGlyphs() {
        ArsZero.LOGGER.debug("Starting glyph registration...");
        
        ArsZero.LOGGER.debug("Registering TemporalContextForm glyph...");
        GlyphRegistry.registerSpell(TEMPORAL_CONTEXT_FORM);
        ArsZero.LOGGER.debug("Successfully registered TemporalContextForm glyph with ID: {}", TEMPORAL_CONTEXT_FORM.getRegistryName());
        
        ArsZero.LOGGER.debug("Registering NearForm glyph...");
        GlyphRegistry.registerSpell(NEAR_FORM);
        ArsZero.LOGGER.debug("Successfully registered NearForm glyph with ID: {}", NEAR_FORM.getRegistryName());
        
        ArsZero.LOGGER.debug("Registering ConjureVoxelEffect glyph...");
        GlyphRegistry.registerSpell(CONJURE_VOXEL_EFFECT);
        ArsZero.LOGGER.debug("Successfully registered ConjureVoxelEffect glyph with ID: {}", CONJURE_VOXEL_EFFECT.getRegistryName());
        
        ArsZero.LOGGER.debug("Registering SelectEffect glyph...");
        GlyphRegistry.registerSpell(SELECT_EFFECT);
        ArsZero.LOGGER.debug("Successfully registered SelectEffect glyph with ID: {}", SELECT_EFFECT.getRegistryName());
        
        ArsZero.LOGGER.debug("Registering AnchorEffect glyph...");
        GlyphRegistry.registerSpell(ANCHOR_EFFECT);
        ArsZero.LOGGER.debug("Successfully registered AnchorEffect glyph with ID: {}", ANCHOR_EFFECT.getRegistryName());
        
        ArsZero.LOGGER.debug("Registering PushEffect glyph...");
        GlyphRegistry.registerSpell(PUSH_EFFECT);
        ArsZero.LOGGER.debug("Successfully registered PushEffect glyph with ID: {}", PUSH_EFFECT.getRegistryName());
        
        ArsZero.LOGGER.debug("Registering RemoveGravity glyph...");
        GlyphRegistry.registerSpell(ZERO_GRAVITY_EFFECT);
        ArsZero.LOGGER.debug("Successfully registered RemoveGravity glyph with ID: {}", ZERO_GRAVITY_EFFECT.getRegistryName());
        
        ArsZero.LOGGER.debug("Glyph registration completed");
    }
}
