package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.glyph.ConjureVoxelEffect;
import com.github.ars_zero.common.glyph.NearForm;
import com.github.ars_zero.common.glyph.PushEffect;
import com.github.ars_zero.common.glyph.SelectEffect;
import com.github.ars_zero.common.glyph.TemporalContextForm;
import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.common.glyph.ZeroGravityEffect;
import com.github.ars_zero.common.glyph.EffectConvergence;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;

public class ModGlyphs {
    
    public static final TemporalContextForm TEMPORAL_CONTEXT_FORM = new TemporalContextForm();
    public static final NearForm NEAR_FORM = new NearForm();
    public static final ConjureVoxelEffect CONJURE_VOXEL_EFFECT = new ConjureVoxelEffect();
    public static final SelectEffect SELECT_EFFECT = new SelectEffect();
    public static final AnchorEffect ANCHOR_EFFECT = new AnchorEffect();
    public static final PushEffect PUSH_EFFECT = new PushEffect();
    public static final ZeroGravityEffect ZERO_GRAVITY_EFFECT = new ZeroGravityEffect();
    public static final EffectConvergence EFFECT_CONVERGENCE = new EffectConvergence();

    public static void registerGlyphs() {
        GlyphRegistry.registerSpell(TEMPORAL_CONTEXT_FORM);
        GlyphRegistry.registerSpell(NEAR_FORM);
        GlyphRegistry.registerSpell(CONJURE_VOXEL_EFFECT);
        GlyphRegistry.registerSpell(SELECT_EFFECT);
        GlyphRegistry.registerSpell(ANCHOR_EFFECT);
        GlyphRegistry.registerSpell(PUSH_EFFECT);
        GlyphRegistry.registerSpell(ZERO_GRAVITY_EFFECT);
        GlyphRegistry.registerSpell(EFFECT_CONVERGENCE);
    }
}
