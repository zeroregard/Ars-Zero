package com.github.ars_zero.common.spell;

import com.github.ars_zero.common.glyph.augment.AugmentAmplifyThree;
import com.github.ars_zero.common.glyph.augment.AugmentAmplifyTwo;
import com.github.ars_zero.common.glyph.augment.AugmentAOEThree;
import com.github.ars_zero.common.glyph.augment.AugmentAOETwo;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class SpellAugmentExtractor {
    private SpellAugmentExtractor() {
    }

    public static final class AugmentData {
        public final int aoeLevel;
        public final int amplifyLevel;
        public final int dampenLevel;

        public AugmentData(int aoeLevel, int amplifyLevel, int dampenLevel) {
            this.aoeLevel = aoeLevel;
            this.amplifyLevel = amplifyLevel;
            this.dampenLevel = dampenLevel;
        }
    }

    /**
     * Extracts augments that are applicable to a target effect from a spell context.
     * Collects augments that appear after the target effect until another effect is found.
     * Returns a structured object with AOE level, Amplify level (sum of tier contributions:
     * AOE/Amplify = 1, AOE II/Amplify II = 2, AOE III/Amplify III = 4), and Dampen count.
     *
     * @param context The spell context to extract from
     * @param targetEffect The target effect to extract augments for
     * @return AugmentData containing aoeLevel, amplifyLevel (levels, not counts), and dampenLevel (count)
     */
    @NotNull
    public static AugmentData extractApplicableAugments(SpellContext context, AbstractEffect targetEffect) {
        ResourceLocation targetId = targetEffect.getRegistryName();
        
        SpellContext iterator = context.clone();
        boolean foundTarget = false;
        
        int aoeLevel = 0;
        int amplifyLevel = 0;
        int dampenLevel = 0;
        
        while (iterator.hasNextPart()) {
            AbstractSpellPart part = iterator.nextPart();
            
            if (!foundTarget) {
                if (part instanceof AbstractEffect effect && effectsMatch(effect, targetEffect, targetId)) {
                    foundTarget = true;
                }
                continue;
            }
            
            if (part instanceof AbstractEffect) {
                break;
            }
            
            if (part instanceof AbstractAugment augment) {
                if (augment == AugmentAOE.INSTANCE) {
                    aoeLevel += 1;
                } else if (augment == AugmentAOETwo.INSTANCE) {
                    aoeLevel += 2;
                } else if (augment == AugmentAOEThree.INSTANCE) {
                    aoeLevel += 4;
                } else if (augment == AugmentAmplify.INSTANCE) {
                    amplifyLevel += 1;
                } else if (augment == AugmentAmplifyTwo.INSTANCE) {
                    amplifyLevel += 2;
                } else if (augment == AugmentAmplifyThree.INSTANCE) {
                    amplifyLevel += 4;
                } else if (augment == AugmentDampen.INSTANCE) {
                    dampenLevel++;
                }
            }
        }
        
        return new AugmentData(aoeLevel, amplifyLevel, dampenLevel);
    }
    
    private static boolean effectsMatch(AbstractEffect candidate, AbstractEffect reference, ResourceLocation id) {
        if (candidate == reference) {
            return true;
        }
        ResourceLocation candidateId = candidate.getRegistryName();
        return id != null && id.equals(candidateId);
    }
}

