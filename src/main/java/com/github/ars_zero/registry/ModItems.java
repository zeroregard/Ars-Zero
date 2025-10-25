package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.ArsZeroStaff;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemRegistryWrapper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ArsZero.MOD_ID);
    
    // Flag to track if spell casters have been registered
    public static boolean SPELL_CASTERS_REGISTERED = false;
    
    public static final ItemRegistryWrapper<ArsZeroStaff> ARS_ZERO_STAFF = register("ars_zero_staff", () -> {
        ArsZero.LOGGER.debug("Creating ArsZeroStaff item instance for registration");
        return new ArsZeroStaff();
    });
    
    public static final DeferredHolder<Item, BlockItem> ARCANE_VOXEL_SPAWNER = ITEMS.register(
        "arcane_voxel_spawner",
        () -> new BlockItem(ModBlocks.ARCANE_VOXEL_SPAWNER.get(), defaultItemProperties())
    );
    
    public static final DeferredHolder<Item, BlockItem> FIRE_VOXEL_SPAWNER = ITEMS.register(
        "fire_voxel_spawner",
        () -> new BlockItem(ModBlocks.FIRE_VOXEL_SPAWNER.get(), defaultItemProperties())
    );
    
    public static final DeferredHolder<Item, BlockItem> WATER_VOXEL_SPAWNER = ITEMS.register(
        "water_voxel_spawner",
        () -> new BlockItem(ModBlocks.WATER_VOXEL_SPAWNER.get(), defaultItemProperties())
    );

    private static <T extends Item> ItemRegistryWrapper<T> register(String name, java.util.function.Supplier<T> item) {
        ArsZero.LOGGER.debug("Registering item: {}", name);
        return new ItemRegistryWrapper<>(ITEMS.register(name, item));
    }

    public static Item.Properties defaultItemProperties() {
        return new Item.Properties();
    }

    public static void registerSpellCasters() {
        ArsZero.LOGGER.info("Registering Ars Zero staff with SpellCasterRegistry");
        ArsZero.LOGGER.debug("Staff item: {}", ARS_ZERO_STAFF.get());
        // Register our staff with the SpellCasterRegistry so Ars Nouveau can detect it
        // Use the same pattern as Ars Nouveau items - extract SpellCaster from data component
        SpellCasterRegistry.register(ARS_ZERO_STAFF.get(), (stack) -> {
            // Extract SpellCaster from the data component (same as Ars Nouveau items)
            return stack.get(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.SPELL_CASTER);
        });
        ArsZero.LOGGER.info("SpellCasterRegistry registration completed");
    }
}
