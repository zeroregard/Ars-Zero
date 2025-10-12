package com.github.ars_zero.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.registry.ModItems;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ArsZero.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ModEventHandlers {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        // This event fires every tick for each player
        // We only need to register once, so we'll use a static flag
        if (!ModItems.SPELL_CASTERS_REGISTERED) {
            ArsZero.LOGGER.info("PlayerTick event fired - registering spell casters with Ars Nouveau...");
            ModItems.registerSpellCasters();
            ModItems.SPELL_CASTERS_REGISTERED = true;
            ArsZero.LOGGER.info("Spell caster registration completed");
        }
    }
}
