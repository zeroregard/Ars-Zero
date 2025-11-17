package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.item.ArchmageSpellStaff;
import com.github.ars_zero.common.item.CreativeSpellStaff;
import com.github.ars_zero.common.item.MageSpellStaff;
import com.github.ars_zero.common.item.NoviceSpellStaff;
import com.github.ars_zero.common.item.SpellcastingCirclet;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemRegistryWrapper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ArsZero.MOD_ID);
    
    public static boolean SPELL_CASTERS_REGISTERED = false;
    
    public static final ItemRegistryWrapper<NoviceSpellStaff> NOVICE_SPELL_STAFF = register("novice_spell_staff", NoviceSpellStaff::new);
    
    public static final ItemRegistryWrapper<MageSpellStaff> MAGE_SPELL_STAFF = register("mage_spell_staff", MageSpellStaff::new);
    
    public static final ItemRegistryWrapper<ArchmageSpellStaff> ARCHMAGE_SPELL_STAFF = register("archmage_spell_staff", ArchmageSpellStaff::new);
    
    public static final ItemRegistryWrapper<CreativeSpellStaff> CREATIVE_SPELL_STAFF = register("creative_spell_staff", CreativeSpellStaff::new);
    
    public static final ItemRegistryWrapper<SpellcastingCirclet> SPELLCASTING_CIRCLET = register("spellcasting_circlet", SpellcastingCirclet::new);
    
    public static final ItemRegistryWrapper<Item> DULL_CIRCLET = register("dull_circlet", () -> new Item(defaultItemProperties()));
    
    public static final ItemRegistryWrapper<Item> ARCHWOOD_ROD = register("archwood_rod", () -> new Item(defaultItemProperties()));
    
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
        ArsZero.LOGGER.info("Registering Ars Zero staves with SpellCasterRegistry");
        registerStaff(NOVICE_SPELL_STAFF.get());
        registerStaff(MAGE_SPELL_STAFF.get());
        registerStaff(ARCHMAGE_SPELL_STAFF.get());
        registerStaff(CREATIVE_SPELL_STAFF.get());
        registerDevice(SPELLCASTING_CIRCLET.get());
        ArsZero.LOGGER.info("SpellCasterRegistry registration completed");
    }
    
    private static void registerStaff(AbstractSpellStaff staff) {
        SpellCasterRegistry.register(staff, (stack) -> {
            return stack.get(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.SPELL_CASTER);
        });
    }
    
    private static void registerDevice(com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice device) {
        SpellCasterRegistry.register(device, (stack) -> {
            return stack.get(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.SPELL_CASTER);
        });
    }
}
