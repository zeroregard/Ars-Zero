package com.github.ars_zero.common.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.gui.documentation.GravityGunExamplePage;
import com.github.ars_zero.registry.ModGlyphs;
import com.github.ars_zero.registry.ModItems;
import com.hollingsworth.arsnouveau.api.documentation.DocCategory;
import com.hollingsworth.arsnouveau.api.documentation.ReloadDocumentationEvent;
import com.hollingsworth.arsnouveau.api.documentation.entry.DocEntry;
import com.hollingsworth.arsnouveau.api.documentation.builder.DocEntryBuilder;
import com.hollingsworth.arsnouveau.api.registry.DocumentationRegistry;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.setup.registry.Documentation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = ArsZero.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class DocumentationEvents {
    private static final int CATEGORY_ORDER = 950;

    @SubscribeEvent
    public static void onAddDocs(ReloadDocumentationEvent.AddEntries event) {
        DocCategory category = new DocCategory(
                ArsZero.prefix("ars_zero"),
                ModItems.NOVICE_SPELL_STAFF.get().getDefaultInstance(),
                CATEGORY_ORDER
        );
        DocumentationRegistry.registerMainCategory(category);

        Documentation.addPage(new DocEntryBuilder(ArsZero.MOD_ID, category, "overview")
                .withIcon(ModItems.NOVICE_SPELL_STAFF.get())
                .withSortNum(1)
                .withIntroPage()
                .withLocalizedText());

        DocEntry spellPhasesEntry = Documentation.addPage(new DocEntryBuilder(ArsZero.MOD_ID, category, "spell_phases")
                .withIcon(ModItems.MULTIPHASE_ORB.get())
                .withSortNum(2)
                .withIntroPage()
                .withLocalizedText());

        DocEntry phaseTargetingEntry = Documentation.addPage(new DocEntryBuilder(ArsZero.MOD_ID, category, "phase_targeting")
                .withIcon(ModGlyphs.TEMPORAL_CONTEXT_FORM.getGlyph())
                .withSortNum(3)
                .withIntroPage()
                .withLocalizedText());

        spellPhasesEntry.withRelation(phaseTargetingEntry.id());

        List<ItemStack> begin = gravityGunBeginGlyphs();
        List<ItemStack> tick = gravityGunTickGlyphs();
        List<ItemStack> end = gravityGunEndGlyphs();
        Documentation.addPage(new DocEntryBuilder(ArsZero.MOD_ID, category, "examples")
                .withIcon(ModItems.SPELLCASTING_CIRCLET.get())
                .withSortNum(4)
                .withPage(GravityGunExamplePage.create(
                        Component.translatable("ars_zero.examples.gravity_gun"),
                        Component.translatable("ars_zero.examples.gravity_gun.quote"),
                        begin, tick, end,
                        Component.translatable("ars_zero.tooltip.begin_phase"),
                        Component.translatable("ars_zero.tooltip.tick_phase"),
                        Component.translatable("ars_zero.tooltip.end_phase"))));
    }

    private static List<ItemStack> gravityGunBeginGlyphs() {
        List<ItemStack> stacks = new ArrayList<>();
        addGlyphStack(stacks, ResourceLocation.fromNamespaceAndPath("ars_nouveau", "glyph_projectile"));
        addGlyphStack(stacks, ArsZero.prefix("select_effect"));
        return stacks;
    }

    private static List<ItemStack> gravityGunTickGlyphs() {
        List<ItemStack> stacks = new ArrayList<>();
        addGlyphStack(stacks, ArsZero.prefix("temporal_context_form"));
        addGlyphStack(stacks, ArsZero.prefix("anchor_effect"));
        return stacks;
    }

    private static List<ItemStack> gravityGunEndGlyphs() {
        List<ItemStack> stacks = new ArrayList<>();
        addGlyphStack(stacks, ArsZero.prefix("temporal_context_form"));
        addGlyphStack(stacks, ArsZero.prefix("push_effect"));
        return stacks;
    }

    private static void addGlyphStack(List<ItemStack> stacks, ResourceLocation glyphId) {
        AbstractSpellPart part = GlyphRegistry.getSpellPart(glyphId);
        if (part != null && part.getGlyph() != null) {
            stacks.add(part.getGlyph().getDefaultInstance());
        }
    }
}
