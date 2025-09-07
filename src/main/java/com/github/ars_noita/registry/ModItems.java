package com.github.ars_noita.registry;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.common.item.ArsNoitaStaff;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemRegistryWrapper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ArsNoita.MOD_ID);
    
    // Flag to track if spell casters have been registered
    public static boolean SPELL_CASTERS_REGISTERED = false;
    
    public static final ItemRegistryWrapper<ArsNoitaStaff> ARS_NOITA_STAFF = register("ars_noita_staff", () -> {
        ArsNoita.LOGGER.debug("Creating ArsNoitaStaff item instance for registration");
        return new ArsNoitaStaff();
    });

    private static <T extends Item> ItemRegistryWrapper<T> register(String name, java.util.function.Supplier<T> item) {
        ArsNoita.LOGGER.debug("Registering item: {}", name);
        return new ItemRegistryWrapper<>(ITEMS.register(name, item));
    }

    public static Item.Properties defaultItemProperties() {
        return new Item.Properties();
    }

    public static void registerSpellCasters() {
        ArsNoita.LOGGER.info("Registering Ars Noita staff with SpellCasterRegistry");
        ArsNoita.LOGGER.debug("Staff item: {}", ARS_NOITA_STAFF.get());
        // Register our staff with the SpellCasterRegistry so Ars Nouveau can detect it
        // Use the same pattern as Ars Nouveau items - extract SpellCaster from data component
        SpellCasterRegistry.register(ARS_NOITA_STAFF.get(), (stack) -> {
            // Extract SpellCaster from the data component (same as Ars Nouveau items)
            return stack.get(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.SPELL_CASTER);
        });
        ArsNoita.LOGGER.info("SpellCasterRegistry registration completed");
    }
}
