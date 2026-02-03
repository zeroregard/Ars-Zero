package com.github.ars_zero.common.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.gui.documentation.MultispellExamplePage;
import com.github.ars_zero.registry.ModItems;
import com.hollingsworth.arsnouveau.api.documentation.DocCategory;
import com.hollingsworth.arsnouveau.api.documentation.builder.DocEntryBuilder;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ExamplesDocHelper {

    private ExamplesDocHelper() {
    }

    public static DocEntryBuilder buildEntry(DocCategory category) {
        DocEntryBuilder builder = new DocEntryBuilder(ArsZero.MOD_ID, category, "examples")
                .withIcon(ModItems.SPELLCASTING_CIRCLET.get())
                .withSortNum(4);
        builder = addExampleSpell(builder, "ars_zero.examples.gravity_gun", "ars_zero.examples.gravity_gun.quote",
                glyphStacks("ars_nouveau:glyph_projectile", "ars_zero:select_effect"),
                glyphStacks("ars_zero:temporal_context_form", "ars_zero:anchor_effect"),
                glyphStacks("ars_zero:temporal_context_form", "ars_zero:push_effect"));
        builder = addExampleSpell(builder, "ars_zero.examples.tier2_teleportation", "ars_zero.examples.tier2_teleportation.quote",
                glyphStacks("ars_zero:near_form", "ars_zero:conjure_voxel_effect"),
                glyphStacks("ars_zero:temporal_context_form", "ars_zero:anchor_effect", "ars_nouveau:glyph_extract"),
                glyphStacks("ars_zero:temporal_context_form", "ars_nouveau:glyph_exchange", "ars_zero:discard_effect"));
        builder = addExampleSpell(builder, "ars_zero.examples.ekusupuroshon", "ars_zero.examples.ekusupuroshon.quote",
                glyphStacks("ars_nouveau:glyph_projectile", "ars_zero:effect_convergence", "ars_nouveau:glyph_explosion",
                        "ars_nouveau:glyph_amplify", "ars_nouveau:glyph_amplify", "ars_nouveau:glyph_amplify",
                        "ars_nouveau:glyph_aoe", "ars_nouveau:glyph_aoe", "ars_nouveau:glyph_aoe", "ars_nouveau:glyph_aoe"),
                glyphStacks("ars_zero:temporal_context_form", "ars_zero:sustain_effect"),
                glyphStacks());
        builder = addExampleSpell(builder, "ars_zero.examples.mage_march", "ars_zero.examples.mage_march.quote",
                glyphStacks(),
                glyphStacks("ars_nouveau:glyph_underfoot", "ars_nouveau:glyph_phantom_block", "ars_nouveau:glyph_aoe", "ars_nouveau:glyph_aoe"),
                glyphStacks());
        return builder;
    }

    private static DocEntryBuilder addExampleSpell(DocEntryBuilder builder, String titleKey, String quoteKey,
                                                   List<ItemStack> beginGlyphs, List<ItemStack> tickGlyphs, List<ItemStack> endGlyphs) {
        return builder.withPage(MultispellExamplePage.create(
                Component.translatable(titleKey),
                Component.translatable(quoteKey),
                beginGlyphs != null ? beginGlyphs : List.of(),
                tickGlyphs != null ? tickGlyphs : List.of(),
                endGlyphs != null ? endGlyphs : List.of(),
                Component.translatable("ars_zero.tooltip.begin_phase"),
                Component.translatable("ars_zero.tooltip.tick_phase"),
                Component.translatable("ars_zero.tooltip.end_phase")));
    }

    private static List<ItemStack> glyphStacks(String... glyphIds) {
        List<ItemStack> stacks = new ArrayList<>();
        for (String id : glyphIds) {
            AbstractSpellPart part = GlyphRegistry.getSpellPart(ResourceLocation.parse(id));
            if (part != null && part.getGlyph() != null) {
                stacks.add(part.getGlyph().getDefaultInstance());
            }
        }
        return stacks;
    }
}
