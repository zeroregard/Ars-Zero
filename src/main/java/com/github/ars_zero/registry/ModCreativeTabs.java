package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArsZero.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARS_ZERO_TAB = TABS.register("general", () -> {
        ArsZero.LOGGER.debug("Creating Ars Zero creative tab");
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.ars_zero"))
                .icon(() -> {
                    ArsZero.LOGGER.debug("Getting icon for Ars Zero creative tab");
                    return ModItems.CREATIVE_SPELL_STAFF.get().getDefaultInstance();
                })
                .displayItems((params, output) -> {
                    ArsZero.LOGGER.info("Populating Ars Zero creative tab with items");
                    try {
                        output.accept(ModItems.NOVICE_SPELL_STAFF.get().getDefaultInstance());
                        output.accept(ModItems.MAGE_SPELL_STAFF.get().getDefaultInstance());
                        output.accept(ModItems.ARCHMAGE_SPELL_STAFF.get().getDefaultInstance());
                        output.accept(ModItems.CREATIVE_SPELL_STAFF.get().getDefaultInstance());
                        output.accept(ModItems.DULL_CIRCLET.get().getDefaultInstance());
                        output.accept(ModItems.SPELLCASTING_CIRCLET.get().getDefaultInstance());
                        output.accept(ModItems.ARCANE_VOXEL_SPAWNER.get().getDefaultInstance());
                        output.accept(ModItems.FIRE_VOXEL_SPAWNER.get().getDefaultInstance());
                        output.accept(ModItems.WATER_VOXEL_SPAWNER.get().getDefaultInstance());
                        ArsZero.LOGGER.info("Attempting to add Multiphase Spell Turret to creative tab");
                        output.accept(ModItems.MULTIPHASE_SPELL_TURRET.get().getDefaultInstance());
                        ArsZero.LOGGER.info("Successfully added Multiphase Spell Turret to creative tab");
                    } catch (Exception e) {
                        ArsZero.LOGGER.error("Error populating Ars Zero creative tab", e);
                    }
                })
                .build();
    });
}
