package com.github.ars_noita.registry;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.common.glyph.TemporalContextForm;
import com.hollingsworth.arsnouveau.setup.registry.GlyphRegistryWrapper;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModGlyphs {
    public static final DeferredRegister<com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart> GLYPHS = 
        DeferredRegister.create(Registries.SPELL_PART, ArsNoita.MOD_ID);

    public static final GlyphRegistryWrapper<TemporalContextForm> TEMPORAL_CONTEXT_FORM = 
        register("temporal_context_form", TemporalContextForm::new);

    private static <T extends com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart> GlyphRegistryWrapper<T> 
        register(String name, java.util.function.Supplier<T> glyph) {
        return new GlyphRegistryWrapper<>(GLYPHS.register(name, glyph));
    }
}
