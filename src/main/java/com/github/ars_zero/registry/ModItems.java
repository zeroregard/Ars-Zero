package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.renderer.item.MultiphaseTurretItemRenderer;
import com.github.ars_zero.registry.ModEntities;
import com.github.ars_zero.common.item.ArchmageSpellStaff;
import com.github.ars_zero.common.item.CreativeSpellStaff;
import com.github.ars_zero.common.item.DullCirclet;
import com.github.ars_zero.common.item.FilialItem;
import com.github.ars_zero.common.item.MageSpellStaff;
import com.github.ars_zero.common.item.MultiphaseOrbItem;
import com.github.ars_zero.common.item.MultiphaseSpellParchment;
import com.github.ars_zero.common.item.NoviceSpellStaff;
import com.github.ars_zero.common.item.SpellcastingCirclet;
import com.alexthw.sauce.registry.ModRegistry;
import com.hollingsworth.arsnouveau.common.items.RendererBlockItem;
import com.hollingsworth.arsnouveau.setup.registry.ItemRegistryWrapper;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ArsZero.MOD_ID);

    public static boolean SPELL_CASTERS_REGISTERED = false;

    static {
        ModStaffItems.register(ITEMS);
    }

    public static final ItemRegistryWrapper<NoviceSpellStaff> NOVICE_SPELL_STAFF = ModStaffItems.NOVICE_SPELL_STAFF;
    public static final ItemRegistryWrapper<MageSpellStaff> MAGE_SPELL_STAFF = ModStaffItems.MAGE_SPELL_STAFF;
    public static final ItemRegistryWrapper<ArchmageSpellStaff> ARCHMAGE_SPELL_STAFF = ModStaffItems.ARCHMAGE_SPELL_STAFF;
    public static final ItemRegistryWrapper<CreativeSpellStaff> CREATIVE_SPELL_STAFF = ModStaffItems.CREATIVE_SPELL_STAFF;
    public static final ItemRegistryWrapper<SpellcastingCirclet> SPELLCASTING_CIRCLET = ModStaffItems.SPELLCASTING_CIRCLET;
    
    public static final ItemRegistryWrapper<DullCirclet> DULL_CIRCLET = register("dull_circlet", () -> new DullCirclet(defaultItemProperties()));
    
    public static final ItemRegistryWrapper<Item> ARCHWOOD_ROD = register("archwood_rod", () -> new Item(defaultItemProperties()));

    public static final ItemRegistryWrapper<MultiphaseSpellParchment> MULTIPHASE_SPELL_PARCHMENT = register("multiphase_spell_parchment", () -> new MultiphaseSpellParchment(defaultItemProperties()));
    
    public static final ItemRegistryWrapper<MultiphaseOrbItem> MULTIPHASE_ORB = register("multiphase_orb", () -> new MultiphaseOrbItem(defaultItemProperties()));
    
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
    
    public static final DeferredHolder<Item, BlockItem> WIND_VOXEL_SPAWNER = ITEMS.register(
        "wind_voxel_spawner",
        () -> new BlockItem(ModBlocks.WIND_VOXEL_SPAWNER.get(), defaultItemProperties())
    );
    
    public static final DeferredHolder<Item, BlockItem> STONE_VOXEL_SPAWNER = ITEMS.register(
        "stone_voxel_spawner",
        () -> new BlockItem(ModBlocks.STONE_VOXEL_SPAWNER.get(), defaultItemProperties())
    );
    
    public static final DeferredHolder<Item, BlockItem> ICE_VOXEL_SPAWNER = ITEMS.register(
        "ice_voxel_spawner",
        () -> new BlockItem(ModBlocks.ICE_VOXEL_SPAWNER.get(), defaultItemProperties())
    );
    
    public static final DeferredHolder<Item, BlockItem> LIGHTNING_VOXEL_SPAWNER = ITEMS.register(
        "lightning_voxel_spawner",
        () -> new BlockItem(ModBlocks.LIGHTNING_VOXEL_SPAWNER.get(), defaultItemProperties())
    );

    public static final DeferredHolder<Item, BlockItem> BLIGHT_VOXEL_SPAWNER = ITEMS.register(
        "blight_voxel_spawner",
        () -> new BlockItem(ModBlocks.BLIGHT_VOXEL_SPAWNER.get(), defaultItemProperties())
    );
    
    public static final DeferredHolder<Item, BlockItem> FROZEN_BLIGHT = ITEMS.register(
        "frozen_blight",
        () -> new BlockItem(ModBlocks.FROZEN_BLIGHT.get(), defaultItemProperties())
    );

    public static final DeferredHolder<Item, BlockItem> STAFF_DISPLAY = ITEMS.register(
        "staff_display",
        () -> new BlockItem(ModBlocks.STAFF_DISPLAY.get(), defaultItemProperties())
    );

    public static final DeferredHolder<Item, BlockItem> BLIGHT_ARCHWOOD_LOG = ITEMS.register(
        "blight_archwood_log",
        () -> new BlockItem(ModBlocks.BLIGHT_ARCHWOOD_LOG.get(), defaultItemProperties())
    );

    public static final DeferredHolder<Item, BlockItem> BLIGHT_ARCHWOOD_LEAVES = ITEMS.register(
        "blight_archwood_leaves",
        () -> new BlockItem(ModBlocks.BLIGHT_ARCHWOOD_LEAVES.get(), defaultItemProperties())
    );

    public static final DeferredHolder<Item, RendererBlockItem> MULTIPHASE_SPELL_TURRET = ITEMS.register(
        "multiphase_spell_turret",
        () -> {
            ArsZero.LOGGER.debug("Registering Multiphase Spell Turret item");
            RendererBlockItem item = new RendererBlockItem(ModBlocks.MULTIPHASE_SPELL_TURRET.get(), defaultItemProperties()) {
                @Override
                @OnlyIn(Dist.CLIENT)
                public Supplier<BlockEntityWithoutLevelRenderer> getRenderer() {
                    return MultiphaseTurretItemRenderer.getISTER();
                }
            };
            ArsZero.LOGGER.debug("Multiphase Spell Turret item created successfully");
            return item;
        }
    );

    public static final ItemRegistryWrapper<com.github.ars_zero.common.item.StaticStaff> STAFF_TELEKINESIS = ModStaffItems.STAFF_TELEKINESIS;

    // -------------------------------------------------------------------------
    // Filial items — one per school of magic
    // -------------------------------------------------------------------------
    public static final ItemRegistryWrapper<FilialItem> FIRE_FILIAL =
        register("fire_filial", () -> new FilialItem("fire", ModRegistry.FIRE_POWER));
    public static final ItemRegistryWrapper<FilialItem> WATER_FILIAL =
        register("water_filial", () -> new FilialItem("water", ModRegistry.WATER_POWER));
    public static final ItemRegistryWrapper<FilialItem> AIR_FILIAL =
        register("air_filial", () -> new FilialItem("air", ModRegistry.AIR_POWER));
    public static final ItemRegistryWrapper<FilialItem> EARTH_FILIAL =
        register("earth_filial", () -> new FilialItem("earth", ModRegistry.EARTH_POWER));
    public static final ItemRegistryWrapper<FilialItem> NECROMANCY_FILIAL =
        register("necromancy_filial", () -> new FilialItem("necromancy", ModRegistry.NECROMANCY_POWER));
    public static final ItemRegistryWrapper<FilialItem> ABJURATION_FILIAL =
        register("abjuration_filial", () -> new FilialItem("abjuration", ModRegistry.ABJURATION_POWER));
    public static final ItemRegistryWrapper<FilialItem> CONJURATION_FILIAL =
        register("conjuration_filial", () -> new FilialItem("conjuration", ModRegistry.CONJURATION_POWER));
    public static final ItemRegistryWrapper<FilialItem> MANIPULATION_FILIAL =
        register("manipulation_filial", () -> new FilialItem("manipulation", ModRegistry.MANIPULATION_POWER));

    /** All 8 filials in school order — used by datagen and other utilities. */
    public static final List<ItemRegistryWrapper<FilialItem>> ALL_FILIALS = List.of(
        FIRE_FILIAL, WATER_FILIAL, AIR_FILIAL, EARTH_FILIAL,
        NECROMANCY_FILIAL, ABJURATION_FILIAL, CONJURATION_FILIAL, MANIPULATION_FILIAL
    );

    /** Spawn eggs for blighted skeleton tiers. */
    public static final DeferredHolder<Item, SpawnEggItem> ACOLYTE_SPAWN_EGG = ITEMS.register(
            "acolyte_spawn_egg",
            () -> new SpawnEggItem(ModEntities.ACOLYTE.get(), 0x8B7355, 0x494949, defaultItemProperties()));
    public static final DeferredHolder<Item, SpawnEggItem> NECROMANCER_SPAWN_EGG = ITEMS.register(
            "necromancer_spawn_egg",
            () -> new SpawnEggItem(ModEntities.NECROMANCER.get(), 0xC1C1C1, 0x494949, defaultItemProperties()));
    public static final DeferredHolder<Item, SpawnEggItem> LICH_SPAWN_EGG = ITEMS.register(
            "lich_spawn_egg",
            () -> new SpawnEggItem(ModEntities.LICH.get(), 0xE8DCC8, 0x2D2D2D, defaultItemProperties()));

    private static <T extends Item> ItemRegistryWrapper<T> register(String name, java.util.function.Supplier<T> item) {
        ArsZero.LOGGER.debug("Registering item: {}", name);
        return new ItemRegistryWrapper<>(ITEMS.register(name, item));
    }

    public static Item.Properties defaultItemProperties() {
        return new Item.Properties();
    }

    public static void registerSpellCasters() {
        ModStaffItems.registerSpellCasters();
    }
}
