package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.renderer.item.MultiphaseTurretItemRenderer;
import com.github.ars_zero.common.item.DullCirclet;
import com.github.ars_zero.common.item.MultiphaseOrbItem;
import com.github.ars_zero.common.item.MultiphaseSpellParchment;
import com.hollingsworth.arsnouveau.common.items.RendererBlockItem;
import com.hollingsworth.arsnouveau.setup.registry.ItemRegistryWrapper;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ArsZero.MOD_ID);
    public static boolean SPELL_CASTERS_REGISTERED = false;

    static {
        ModStaffItems.register(ITEMS);
    }

    public static final ItemRegistryWrapper<com.github.ars_zero.common.item.NoviceSpellStaff> NOVICE_SPELL_STAFF = ModStaffItems.NOVICE_SPELL_STAFF;
    public static final ItemRegistryWrapper<com.github.ars_zero.common.item.MageSpellStaff> MAGE_SPELL_STAFF = ModStaffItems.MAGE_SPELL_STAFF;
    public static final ItemRegistryWrapper<com.github.ars_zero.common.item.ArchmageSpellStaff> ARCHMAGE_SPELL_STAFF = ModStaffItems.ARCHMAGE_SPELL_STAFF;
    public static final ItemRegistryWrapper<com.github.ars_zero.common.item.CreativeSpellStaff> CREATIVE_SPELL_STAFF = ModStaffItems.CREATIVE_SPELL_STAFF;
    public static final ItemRegistryWrapper<com.github.ars_zero.common.item.SpellcastingCirclet> SPELLCASTING_CIRCLET = ModStaffItems.SPELLCASTING_CIRCLET;
    public static final ItemRegistryWrapper<com.github.ars_zero.common.item.StaticStaff> STAFF_TELEKINESIS = ModStaffItems.STAFF_TELEKINESIS;
    public static final ItemRegistryWrapper<com.github.ars_zero.common.item.StaticStaff> STAFF_DEMONBANE = ModStaffItems.STAFF_DEMONBANE;
    public static final ItemRegistryWrapper<com.github.ars_zero.common.item.StaticStaff> STAFF_VOXELS = ModStaffItems.STAFF_VOXELS;
    public static final ItemRegistryWrapper<com.github.ars_zero.common.item.StaticStaff> STAFF_GEOMETRIZE = ModStaffItems.STAFF_GEOMETRIZE;
    public static final ItemRegistryWrapper<com.github.ars_zero.common.item.StaticStaff> STAFF_CONVERGENCE = ModStaffItems.STAFF_CONVERGENCE;
    public static final ItemRegistryWrapper<com.github.ars_zero.common.item.StaticStaff> STAFF_LAKES = ModStaffItems.STAFF_LAKES;

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
