package com.github.ars_zero.common.spell;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Objects;

public interface ISubsequentEffectProvider {

    ResourceLocation[] getSubsequentEffectGlyphs();

    default Component createSubsequentGlyphTooltip(ResourceLocation glyphId) {
        return Component.translatable(getSubsequentGlyphTranslationKey(glyphId));
    }

    default String getSubsequentGlyphTranslationKey(ResourceLocation glyphId) {
        ResourceLocation providerId = getProviderId();
        if (providerId == null || glyphId == null) {
            return "ars_zero.effect_augment_desc.missing";
        }
        String effectPath = sanitize(providerId.getPath());
        String glyphPath = sanitize(glyphId.getPath());
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
