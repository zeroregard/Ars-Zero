package com.github.ars_zero.client.gui;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class SpellCompositeContext {
    private static SpellCompositeContext instance;
    
    private List<AbstractSpellPart> currentSpell = new ArrayList<>();
    
    public static SpellCompositeContext getInstance() {
        if (instance == null) {
            instance = new SpellCompositeContext();
        }
        return instance;
    }
    
    public void setCurrentSpell(List<AbstractSpellPart> spell) {
        this.currentSpell = spell != null ? new ArrayList<>(spell) : new ArrayList<>();
    }
    
    public AbstractSpellPart getLastEffect() {
        if (currentSpell.isEmpty()) {
            return null;
        }
        for (int i = currentSpell.size() - 1; i >= 0; i--) {
            AbstractSpellPart part = currentSpell.get(i);
            if (part != null && part.getTypeIndex() == 5) {
                return part;
            }
        }
        return null;
    }
    
    public boolean isSubsequentEffect(ResourceLocation glyphId) {
        AbstractSpellPart lastEffect = getLastEffect();
        if (lastEffect == null) {
            return false;
        }
        if (!(lastEffect instanceof com.github.ars_zero.common.spell.ISubsequentEffectProvider provider)) {
            return false;
        }
        ResourceLocation[] subsequentGlyphs = provider.getSubsequentEffectGlyphs();
        if (subsequentGlyphs == null || subsequentGlyphs.length == 0) {
            return false;
        }
        for (ResourceLocation id : subsequentGlyphs) {
            if (id != null && id.equals(glyphId)) {
                return true;
            }
        }
        return false;
    }
}

