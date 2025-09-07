package com.github.ars_noita.event;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.registry.ModItems;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;

@EventBusSubscriber(modid = ArsNoita.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ModEventHandlers {

    @SubscribeEvent
    public static void onEntityConstructing(EntityEvent.EntityConstructing event) {
        // This event fires after all registries are fully loaded
        // We only need to register once, so we'll use a static flag
        if (!ModItems.SPELL_CASTERS_REGISTERED) {
            ArsNoita.LOGGER.debug("Registering spell casters with Ars Nouveau...");
            ModItems.registerSpellCasters();
            ModItems.SPELL_CASTERS_REGISTERED = true;
            ArsNoita.LOGGER.info("Spell caster registration completed");
        }
    }
}
