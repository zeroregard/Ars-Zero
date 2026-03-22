package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.renderer.item.MultiphaseTurretItemRenderer;
import com.github.ars_zero.common.item.armor.NecroCrownArmor;
import com.github.ars_zero.common.item.armor.RottedArcanistArmor;
import com.github.ars_zero.common.item.armor.TatteredArcanistArmor;
import com.github.ars_zero.registry.ModEntities;
import java.util.LinkedHashMap;
import java.util.Map;
import com.github.ars_zero.common.item.ArchmageSpellStaff;
import com.github.ars_zero.common.item.CreativeFilialItem;
import com.github.ars_zero.common.item.BoneChestItem;
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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
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

    public static final ItemRegistryWrapper<BoneChestItem> BONE_CHEST = register("bone_chest", () -> new BoneChestItem(ModBlocks.BONE_CHEST.get(), defaultItemProperties()));

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

    public static final DeferredHolder<Item, BlockItem> BLIGHTED_SOIL = ITEMS.register(
        "blighted_soil",
        () -> new BlockItem(ModBlocks.BLIGHTED_SOIL.get(), defaultItemProperties())
    );

    public static final DeferredHolder<Item, BlockItem> FROZEN_BLIGHT = ITEMS.register(
        "frozen_blight",
        () -> new BlockItem(ModBlocks.FROZEN_BLIGHT.get(), defaultItemProperties())
    );

    public static final DeferredHolder<Item, BlockItem> STAFF_DISPLAY = ITEMS.register(
        "staff_display",
        () -> new BlockItem(ModBlocks.STAFF_DISPLAY.get(), defaultItemProperties())
    );

    public static final DeferredHolder<Item, BlockItem> OSSUARY_BEACON = ITEMS.register(
        "ossuary_beacon",
        () -> new BlockItem(ModBlocks.OSSUARY_BEACON.get(), defaultItemProperties())
    );

    public static final DeferredHolder<Item, BlockItem> BLIGHT_ARCHWOOD_LOG = ITEMS.register(
        "blight_archwood_log",
        () -> new BlockItem(ModBlocks.BLIGHT_ARCHWOOD_LOG.get(), defaultItemProperties())
    );

    public static final DeferredHolder<Item, BlockItem> BLIGHT_ARCHWOOD_LEAVES = ITEMS.register(
        "blight_archwood_leaves",
        () -> new BlockItem(ModBlocks.BLIGHT_ARCHWOOD_LEAVES.get(), defaultItemProperties())
    );

    public static final DeferredHolder<Item, BlockItem> BLIGHT_VEIN = ITEMS.register(
        "blight_vein",
        () -> new BlockItem(ModBlocks.BLIGHT_VEIN.get(), defaultItemProperties())
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


    // -------------------------------------------------------------------------
    // Corrupted Sourcestone block items
    // -------------------------------------------------------------------------

    public static final Map<String, DeferredHolder<Item, BlockItem>> CORRUPTED_BLOCK_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> CORRUPTED_STAIR_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> CORRUPTED_SLAB_ITEMS = new LinkedHashMap<>();

    static {
        for (String name : ModBlocks.CORRUPTED_BASE_NAMES) {
            CORRUPTED_BLOCK_ITEMS.put(name, ITEMS.register(name,
                    () -> new BlockItem(ModBlocks.CORRUPTED_BLOCKS.get(name).get(), defaultItemProperties())));
            CORRUPTED_STAIR_ITEMS.put(name, ITEMS.register(name + "_stairs",
                    () -> new BlockItem(ModBlocks.CORRUPTED_STAIRS.get(name).get(), defaultItemProperties())));
            CORRUPTED_SLAB_ITEMS.put(name, ITEMS.register(name + "_slab",
                    () -> new BlockItem(ModBlocks.CORRUPTED_SLABS.get(name).get(), defaultItemProperties())));
        }
    }

    // -------------------------------------------------------------------------
    // Filial items — one per school of magic
    // -------------------------------------------------------------------------
    public static final ItemRegistryWrapper<FilialItem> FIRE_FILIAL =
        register("fire_filial", () -> new FilialItem("fire", ModRegistry.FIRE_POWER, null));
    public static final ItemRegistryWrapper<FilialItem> WATER_FILIAL =
        register("water_filial", () -> new FilialItem("water", ModRegistry.WATER_POWER, "animation"));
    public static final ItemRegistryWrapper<FilialItem> AIR_FILIAL =
        register("air_filial", () -> new FilialItem("air", ModRegistry.AIR_POWER, "twister"));
    public static final ItemRegistryWrapper<FilialItem> EARTH_FILIAL =
        register("earth_filial", () -> new FilialItem("earth", ModRegistry.EARTH_POWER, null));
    public static final ItemRegistryWrapper<FilialItem> NECROMANCY_FILIAL =
        register("necromancy_filial", () -> new FilialItem("necromancy", ModRegistry.NECROMANCY_POWER, null).noSpin());
    public static final ItemRegistryWrapper<FilialItem> ABJURATION_FILIAL =
        register("abjuration_filial", () -> new FilialItem("abjuration", ModRegistry.ABJURATION_POWER, null));
    public static final ItemRegistryWrapper<FilialItem> CONJURATION_FILIAL =
        register("conjuration_filial", () -> new FilialItem("conjuration", ModRegistry.SUMMON_POWER, "spinning"));
    public static final ItemRegistryWrapper<FilialItem> MANIPULATION_FILIAL =
        register("manipulation_filial", () -> new FilialItem("manipulation", ModRegistry.MANIPULATION_POWER, null).noSpin());

    // Uncraftable filials
    public static final ItemRegistryWrapper<FilialItem> DEMONBANE_FILIAL =
        register("demonbane_filial", () -> new FilialItem("demonbane", null, null));
    public static final ItemRegistryWrapper<FilialItem> CREATIVE_FILIAL =
        register("creative_filial", CreativeFilialItem::new);

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

    public static final DeferredHolder<Item, SpawnEggItem> BONE_GOLEM_SPAWN_EGG = ITEMS.register(
            "bone_golem_spawn_egg",
            () -> new SpawnEggItem(ModEntities.BONE_GOLEM.get(), 0xE8E8D8, 0x4A3728, defaultItemProperties()));

    // -------------------------------------------------------------------------
    // Tattered Arcanist Armor
    // -------------------------------------------------------------------------

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, ArsZero.MOD_ID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TATTERED_ARCANIST_MATERIAL =
            ARMOR_MATERIALS.register("tattered_arcanist", () -> {
                java.util.EnumMap<ArmorItem.Type, Integer> defense = new java.util.EnumMap<>(ArmorItem.Type.class);
                defense.put(ArmorItem.Type.HELMET,     2);
                defense.put(ArmorItem.Type.CHESTPLATE, 4);
                defense.put(ArmorItem.Type.LEGGINGS,   3);
                defense.put(ArmorItem.Type.BOOTS,      1);
                defense.put(ArmorItem.Type.BODY,       4);
                return new ArmorMaterial(
                        defense,
                        12,
                        net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,
                        () -> Ingredient.EMPTY,
                        List.of(new ArmorMaterial.Layer(ArsZero.prefix("tattered_arcanist"))),
                        0.0f, 0.0f
                );
            });

    public static final DeferredHolder<Item, TatteredArcanistArmor> TATTERED_ARCANIST_HELMET =
            ITEMS.register("tattered_arcanist_helmet",
                    () -> new TatteredArcanistArmor(TATTERED_ARCANIST_MATERIAL, ArmorItem.Type.HELMET));

    public static final DeferredHolder<Item, TatteredArcanistArmor> TATTERED_ARCANIST_CHESTPLATE =
            ITEMS.register("tattered_arcanist_chestplate",
                    () -> new TatteredArcanistArmor(TATTERED_ARCANIST_MATERIAL, ArmorItem.Type.CHESTPLATE));

    public static final DeferredHolder<Item, TatteredArcanistArmor> TATTERED_ARCANIST_LEGGINGS =
            ITEMS.register("tattered_arcanist_leggings",
                    () -> new TatteredArcanistArmor(TATTERED_ARCANIST_MATERIAL, ArmorItem.Type.LEGGINGS));

    public static final DeferredHolder<Item, TatteredArcanistArmor> TATTERED_ARCANIST_BOOTS =
            ITEMS.register("tattered_arcanist_boots",
                    () -> new TatteredArcanistArmor(TATTERED_ARCANIST_MATERIAL, ArmorItem.Type.BOOTS));

    // -------------------------------------------------------------------------
    // Rotted Arcanist Armor
    // -------------------------------------------------------------------------

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ROTTED_ARCANIST_MATERIAL =
            ARMOR_MATERIALS.register("rotted_arcanist", () -> {
                java.util.EnumMap<ArmorItem.Type, Integer> defense = new java.util.EnumMap<>(ArmorItem.Type.class);
                defense.put(ArmorItem.Type.HELMET,     2);
                defense.put(ArmorItem.Type.CHESTPLATE, 4);
                defense.put(ArmorItem.Type.LEGGINGS,   3);
                defense.put(ArmorItem.Type.BOOTS,      1);
                defense.put(ArmorItem.Type.BODY,       4);
                return new ArmorMaterial(
                        defense,
                        12,
                        net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,
                        () -> Ingredient.EMPTY,
                        List.of(new ArmorMaterial.Layer(ArsZero.prefix("rotted_arcanist"))),
                        0.0f, 0.0f
                );
            });

    public static final DeferredHolder<Item, RottedArcanistArmor> ROTTED_ARCANIST_HELMET =
            ITEMS.register("rotted_arcanist_helmet",
                    () -> new RottedArcanistArmor(ROTTED_ARCANIST_MATERIAL, ArmorItem.Type.HELMET));

    public static final DeferredHolder<Item, RottedArcanistArmor> ROTTED_ARCANIST_CHESTPLATE =
            ITEMS.register("rotted_arcanist_chestplate",
                    () -> new RottedArcanistArmor(ROTTED_ARCANIST_MATERIAL, ArmorItem.Type.CHESTPLATE));

    public static final DeferredHolder<Item, RottedArcanistArmor> ROTTED_ARCANIST_LEGGINGS =
            ITEMS.register("rotted_arcanist_leggings",
                    () -> new RottedArcanistArmor(ROTTED_ARCANIST_MATERIAL, ArmorItem.Type.LEGGINGS));

    public static final DeferredHolder<Item, RottedArcanistArmor> ROTTED_ARCANIST_BOOTS =
            ITEMS.register("rotted_arcanist_boots",
                    () -> new RottedArcanistArmor(ROTTED_ARCANIST_MATERIAL, ArmorItem.Type.BOOTS));

    // -------------------------------------------------------------------------
    // Necro Crown (Lich helmet)
    // -------------------------------------------------------------------------

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> NECRO_CROWN_MATERIAL =
            ARMOR_MATERIALS.register("necro_crown", () -> {
                java.util.EnumMap<ArmorItem.Type, Integer> defense = new java.util.EnumMap<>(ArmorItem.Type.class);
                defense.put(ArmorItem.Type.HELMET,     3);
                defense.put(ArmorItem.Type.CHESTPLATE, 0);
                defense.put(ArmorItem.Type.LEGGINGS,   0);
                defense.put(ArmorItem.Type.BOOTS,      0);
                defense.put(ArmorItem.Type.BODY,       0);
                return new ArmorMaterial(
                        defense,
                        0,
                        net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,
                        () -> Ingredient.EMPTY,
                        List.of(new ArmorMaterial.Layer(ArsZero.prefix("necro_crown"))),
                        0.0f, 0.0f
                );
            });

    public static final DeferredHolder<Item, NecroCrownArmor> NECRO_CROWN =
            ITEMS.register("necro_crown",
                    () -> new NecroCrownArmor(NECRO_CROWN_MATERIAL));

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
