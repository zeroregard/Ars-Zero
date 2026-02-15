package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.renderer.item.MultiphaseTurretItemRenderer;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.item.ArchmageSpellStaff;
import com.github.ars_zero.common.item.CreativeSpellStaff;
import com.github.ars_zero.common.item.DullCirclet;
import com.github.ars_zero.common.item.MageSpellStaff;
import com.github.ars_zero.common.item.MultiphaseOrbItem;
import com.github.ars_zero.common.item.MultiphaseSpellParchment;
import com.github.ars_zero.common.item.NoviceSpellStaff;
import com.github.ars_zero.common.item.SpellcastingCirclet;
import com.github.ars_zero.common.item.StaffTelekinesis;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
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
    
    public static final ItemRegistryWrapper<NoviceSpellStaff> NOVICE_SPELL_STAFF = register("novice_spell_staff", NoviceSpellStaff::new);
    
    public static final ItemRegistryWrapper<MageSpellStaff> MAGE_SPELL_STAFF = register("mage_spell_staff", MageSpellStaff::new);
    
    public static final ItemRegistryWrapper<ArchmageSpellStaff> ARCHMAGE_SPELL_STAFF = register("archmage_spell_staff", ArchmageSpellStaff::new);
    
    public static final ItemRegistryWrapper<CreativeSpellStaff> CREATIVE_SPELL_STAFF = register("creative_spell_staff", CreativeSpellStaff::new);
    
    public static final ItemRegistryWrapper<SpellcastingCirclet> SPELLCASTING_CIRCLET = register("spellcasting_circlet", SpellcastingCirclet::new);
    
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

    //
    // "Static" staff items
    //

    public static final ItemRegistryWrapper<StaffTelekinesis> STAFF_TELEKINESIS = register("staff_telekinesis", StaffTelekinesis::new);

    private static <T extends Item> ItemRegistryWrapper<T> register(String name, java.util.function.Supplier<T> item) {
        ArsZero.LOGGER.debug("Registering item: {}", name);
        return new ItemRegistryWrapper<>(ITEMS.register(name, item));
    }

    public static Item.Properties defaultItemProperties() {
        return new Item.Properties();
    }

    public static void registerSpellCasters() {
        ArsZero.LOGGER.debug("Registering Ars Zero staves with SpellCasterRegistry");
        registerStaff(NOVICE_SPELL_STAFF.get());
        registerStaff(MAGE_SPELL_STAFF.get());
        registerStaff(ARCHMAGE_SPELL_STAFF.get());
        registerStaff(CREATIVE_SPELL_STAFF.get());
        registerStaff(STAFF_TELEKINESIS.get());
        registerDevice(SPELLCASTING_CIRCLET.get());
        ArsZero.LOGGER.debug("SpellCasterRegistry registration completed");
    }
    
    private static void registerStaff(AbstractSpellStaff staff) {
        SpellCasterRegistry.register(staff, (stack) -> {
            return stack.get(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.SPELL_CASTER);
        });
    }
    
    private static void registerDevice(com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice device) {
        SpellCasterRegistry.register(device, (stack) -> {
            return stack.get(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.SPELL_CASTER);
        });
    }
}
