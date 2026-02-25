package com.github.ars_zero.common.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.registry.ModGlyphs;
import com.github.ars_zero.registry.ModItems;
import com.hollingsworth.arsnouveau.api.documentation.DocCategory;
import com.hollingsworth.arsnouveau.api.documentation.ReloadDocumentationEvent;
import com.hollingsworth.arsnouveau.api.documentation.entry.DocEntry;
import com.hollingsworth.arsnouveau.api.documentation.entry.TextEntry;
import com.hollingsworth.arsnouveau.api.documentation.builder.DocEntryBuilder;
import com.hollingsworth.arsnouveau.api.registry.DocumentationRegistry;
import com.hollingsworth.arsnouveau.setup.registry.Documentation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;

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
                .withIntroPage());

        DocEntry phaseTargetingEntry = Documentation.addPage(new DocEntryBuilder(ArsZero.MOD_ID, category, "phase_targeting")
                .withIcon(ModGlyphs.TEMPORAL_CONTEXT_FORM.getGlyph())
                .withSortNum(3)
                .withIntroPage());

        spellPhasesEntry.withRelation(phaseTargetingEntry.id());

        DocEntryBuilder lifespanBuilder = new DocEntryBuilder(ArsZero.MOD_ID, category, "lifespan_based_effects")
                .withIcon(ModGlyphs.EFFECT_BEAM.getGlyph())
                .withSortNum(4);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            List<ItemStack> lifespanStacks = lifespanGlyphStacks();
            lifespanBuilder
                    .withPage(com.github.ars_zero.client.gui.documentation.LifespanIntroWithGlyphsPage.create(
                            "ars_zero.page.lifespan_based_effects",
                            "ars_zero.page.lifespan_based_effects.intro",
                            lifespanStacks))
                    .withPage(com.github.ars_zero.client.gui.documentation.LifespanGlyphsOnlyPage.create(lifespanStacks));
        } else {
            lifespanBuilder.withPage(TextEntry.create(
                    Component.translatable("ars_zero.page.lifespan_based_effects.intro"),
                    Component.translatable("ars_zero.page.lifespan_based_effects"),
                    ModGlyphs.EFFECT_BEAM.getGlyph()));
        }
        Documentation.addPage(lifespanBuilder);

        Documentation.addPage(ExamplesDocHelper.buildEntry(category));
    }

    private static List<ItemStack> lifespanGlyphStacks() {
        return List.of(
                ModGlyphs.EFFECT_BEAM.getGlyph().getDefaultInstance(),
                ModGlyphs.EFFECT_CONVERGENCE.getGlyph().getDefaultInstance(),
                ModGlyphs.EFFECT_GEOMETRIZE.getGlyph().getDefaultInstance(),
                ModGlyphs.CONJURE_VOXEL_EFFECT.getGlyph().getDefaultInstance()
        );
    }
}
