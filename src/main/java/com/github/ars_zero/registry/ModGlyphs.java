package com.github.ars_zero.registry;

import com.github.ars_zero.common.glyph.ConjureVoxelEffect;
import com.github.ars_zero.common.glyph.DiscardEffect;
import com.github.ars_zero.common.glyph.EffectConjureBlight;
import com.github.ars_zero.common.glyph.NearForm;
import com.github.ars_zero.common.glyph.EffectBeam;
import com.github.ars_zero.common.glyph.PushEffect;
import com.github.ars_zero.common.glyph.SelectEffect;
import com.github.ars_zero.common.glyph.TemporalContextForm;
import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.common.glyph.SustainEffect;
import com.github.ars_zero.common.glyph.ZeroGravityEffect;
import com.github.ars_zero.common.glyph.augment.AugmentAmplifyThree;
import com.github.ars_zero.common.glyph.augment.AugmentAmplifyTwo;
import com.github.ars_zero.common.glyph.augment.AugmentAOEThree;
import com.github.ars_zero.common.glyph.augment.AugmentAOETwo;
import com.github.ars_zero.common.glyph.augment.AugmentCube;
import com.github.ars_zero.common.glyph.augment.AugmentFlatten;
import com.github.ars_zero.common.glyph.augment.AugmentHollow;
import com.github.ars_zero.common.glyph.augment.AugmentSphere;
import com.github.ars_zero.common.glyph.convergence.EffectConvergence;
import com.github.ars_zero.common.glyph.geometrize.EffectGeometrize;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import net.minecraft.network.chat.Component;

public class ModGlyphs {

    public static final TemporalContextForm TEMPORAL_CONTEXT_FORM = new TemporalContextForm();
    public static final NearForm NEAR_FORM = new NearForm();
    public static final ConjureVoxelEffect CONJURE_VOXEL_EFFECT = new ConjureVoxelEffect();
    public static final SelectEffect SELECT_EFFECT = new SelectEffect();
    public static final AnchorEffect ANCHOR_EFFECT = new AnchorEffect();
    public static final SustainEffect SUSTAIN_EFFECT = new SustainEffect();
    public static final PushEffect PUSH_EFFECT = new PushEffect();
    public static final EffectBeam EFFECT_BEAM = EffectBeam.INSTANCE;
    public static final ZeroGravityEffect ZERO_GRAVITY_EFFECT = new ZeroGravityEffect();
    public static final EffectConvergence EFFECT_CONVERGENCE = new EffectConvergence();
    public static final EffectGeometrize EFFECT_GEOMETRIZE = EffectGeometrize.INSTANCE;
    public static final DiscardEffect DISCARD_EFFECT = new DiscardEffect();
    public static final AugmentHollow AUGMENT_HOLLOW = AugmentHollow.INSTANCE;
    public static final AugmentSphere AUGMENT_SPHERE = AugmentSphere.INSTANCE;
    public static final AugmentCube AUGMENT_CUBE = AugmentCube.INSTANCE;
    public static final AugmentFlatten AUGMENT_FLATTEN = AugmentFlatten.INSTANCE;
    public static final EffectConjureBlight EFFECT_CONJURE_BLIGHT = EffectConjureBlight.INSTANCE;
    public static final AugmentAOETwo AUGMENT_AOE_TWO = AugmentAOETwo.INSTANCE;
    public static final AugmentAOEThree AUGMENT_AOE_THREE = AugmentAOEThree.INSTANCE;
    public static final AugmentAmplifyTwo AUGMENT_AMPLIFY_TWO = AugmentAmplifyTwo.INSTANCE;
    public static final AugmentAmplifyThree AUGMENT_AMPLIFY_THREE = AugmentAmplifyThree.INSTANCE;

    public static void registerGlyphs() {
        GlyphRegistry.registerSpell(TEMPORAL_CONTEXT_FORM);
        GlyphRegistry.registerSpell(NEAR_FORM);
        GlyphRegistry.registerSpell(CONJURE_VOXEL_EFFECT);
        GlyphRegistry.registerSpell(SELECT_EFFECT);
        GlyphRegistry.registerSpell(ANCHOR_EFFECT);
        GlyphRegistry.registerSpell(SUSTAIN_EFFECT);
        GlyphRegistry.registerSpell(PUSH_EFFECT);
        GlyphRegistry.registerSpell(EFFECT_BEAM);
        GlyphRegistry.registerSpell(ZERO_GRAVITY_EFFECT);
        GlyphRegistry.registerSpell(EFFECT_CONVERGENCE);
        GlyphRegistry.registerSpell(EFFECT_GEOMETRIZE);
        GlyphRegistry.registerSpell(DISCARD_EFFECT);
        GlyphRegistry.registerSpell(AUGMENT_HOLLOW);
        GlyphRegistry.registerSpell(AUGMENT_SPHERE);
        GlyphRegistry.registerSpell(AUGMENT_CUBE);
        GlyphRegistry.registerSpell(AUGMENT_FLATTEN);
        GlyphRegistry.registerSpell(EFFECT_CONJURE_BLIGHT);
        GlyphRegistry.registerSpell(AUGMENT_AOE_TWO);
        GlyphRegistry.registerSpell(AUGMENT_AOE_THREE);
        GlyphRegistry.registerSpell(AUGMENT_AMPLIFY_TWO);
        GlyphRegistry.registerSpell(AUGMENT_AMPLIFY_THREE);
    }

    /**
     * Adds AOE II/III and Amplify II/III to every spell part that already accepts AOE or Amplify.
     * Called from common setup so they are treated as compatible wherever the base augment is.
     */
    public static void addOptionalAugmentCompatibility() {
        for (AbstractSpellPart part : GlyphRegistry.getSpellpartMap().values()) {
            if (part.compatibleAugments.contains(AugmentAOE.INSTANCE)) {
                part.compatibleAugments.add(AUGMENT_AOE_TWO);
                part.compatibleAugments.add(AUGMENT_AOE_THREE);
                part.augmentDescriptions.put(AUGMENT_AOE_TWO, Component.translatable("ars_zero.augment_desc.aoe_two_generic"));
                part.augmentDescriptions.put(AUGMENT_AOE_THREE, Component.translatable("ars_zero.augment_desc.aoe_three_generic"));
            }
            if (part.compatibleAugments.contains(AugmentAmplify.INSTANCE)) {
                part.compatibleAugments.add(AUGMENT_AMPLIFY_TWO);
                part.compatibleAugments.add(AUGMENT_AMPLIFY_THREE);
                part.augmentDescriptions.put(AUGMENT_AMPLIFY_TWO, Component.translatable("ars_zero.augment_desc.amplify_two_generic"));
                part.augmentDescriptions.put(AUGMENT_AMPLIFY_THREE, Component.translatable("ars_zero.augment_desc.amplify_three_generic"));
            }
        }
    }
}
