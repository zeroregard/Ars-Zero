package com.github.ars_zero.common.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.registry.ModItems;
import com.hollingsworth.arsnouveau.api.documentation.DocCategory;
import com.hollingsworth.arsnouveau.api.documentation.ReloadDocumentationEvent;
import com.hollingsworth.arsnouveau.api.documentation.builder.DocEntryBuilder;
import com.hollingsworth.arsnouveau.api.registry.DocumentationRegistry;
import com.hollingsworth.arsnouveau.setup.registry.Documentation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

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

        Documentation.addPage(new DocEntryBuilder(ArsZero.MOD_ID, category, "multiphase_spellcasting")
                .withIcon(ModItems.SPELLCASTING_CIRCLET.get())
                .withSortNum(2)
                .withIntroPage()
                .withLocalizedText());

        Documentation.addPage(new DocEntryBuilder(ArsZero.MOD_ID, category, "voxel_interactions")
                .withIcon(ModItems.ARCANE_VOXEL_SPAWNER.get())
                .withSortNum(3)
                .withIntroPage()
                .withLocalizedText());
    }
}
