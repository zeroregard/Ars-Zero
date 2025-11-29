package com.github.ars_zero.common.spell;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Objects;

public interface ISubsequentEffectProvider {

    ResourceLocation[] getSubsequentEffectGlyphs();

    default Component createSubsequentGlyphTooltip(ResourceLocation glyphId) {
        String key = getSubsequentGlyphTranslationKey(glyphId);
        return key != null ? Component.translatable(key) : null;
    }

    default String getSubsequentGlyphTranslationKey(ResourceLocation glyphId) {
        ResourceLocation providerId = getProviderId();
        if (providerId == null || glyphId == null) {
            return null;
        }
        String effectPath = sanitize(providerId.getPath());
        String glyphPath = sanitize(glyphId.getPath());
        
        if (glyphPath.startsWith("glyph_")) {
            glyphPath = glyphPath.substring(6);
        }
        if (glyphPath.endsWith("_glyph")) {
            glyphPath = glyphPath.substring(0, glyphPath.length() - 6);
        }
        
        return "ars_zero.effect_augment_desc." + effectPath + "_glyph_" + glyphPath;
    }

    private ResourceLocation getProviderId() {
        if (this instanceof AbstractSpellPart spellPart) {
            return spellPart.getRegistryName();
        }
        return null;
    }

    private static String sanitize(String value) {
        return Objects.requireNonNullElse(value, "unknown").toLowerCase(Locale.ROOT);
    }
}
