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
                    ArsZero.LOGGER.debug("Populating Ars Zero creative tab with items");
                    output.accept(ModItems.NOVICE_SPELL_STAFF.get().getDefaultInstance());
                    output.accept(ModItems.MAGE_SPELL_STAFF.get().getDefaultInstance());
                    output.accept(ModItems.ARCHMAGE_SPELL_STAFF.get().getDefaultInstance());
                    output.accept(ModItems.CREATIVE_SPELL_STAFF.get().getDefaultInstance());
                    for (var holder : ModStaffItems.getRegisteredStaticStaffs()) {
                        output.accept(holder.get().getDefaultInstance());
                    }
                    for (var filial : ModItems.ALL_FILIALS) {
                        output.accept(filial.get().getDefaultInstance());
                    }
                    output.accept(ModItems.DULL_CIRCLET.get().getDefaultInstance());
                    output.accept(ModItems.SPELLCASTING_CIRCLET.get().getDefaultInstance());
                    output.accept(ModItems.ARCHWOOD_ROD.get().getDefaultInstance());
                    output.accept(ModItems.MULTIPHASE_SPELL_PARCHMENT.get().getDefaultInstance());
                    output.accept(ModItems.MULTIPHASE_ORB.get().getDefaultInstance());
                    output.accept(ModItems.ARCANE_VOXEL_SPAWNER.get().getDefaultInstance());
                    output.accept(ModItems.FIRE_VOXEL_SPAWNER.get().getDefaultInstance());
                    output.accept(ModItems.WATER_VOXEL_SPAWNER.get().getDefaultInstance());
                    output.accept(ModItems.WIND_VOXEL_SPAWNER.get().getDefaultInstance());
                    output.accept(ModItems.STONE_VOXEL_SPAWNER.get().getDefaultInstance());
                    output.accept(ModItems.ICE_VOXEL_SPAWNER.get().getDefaultInstance());
                    output.accept(ModItems.LIGHTNING_VOXEL_SPAWNER.get().getDefaultInstance());
                    output.accept(ModItems.BLIGHT_VOXEL_SPAWNER.get().getDefaultInstance());
                    output.accept(ModItems.BLIGHTED_SOIL.get().getDefaultInstance());
                    output.accept(ModItems.FROZEN_BLIGHT.get().getDefaultInstance());
                    output.accept(ModItems.BLIGHT_ARCHWOOD_LOG.get().getDefaultInstance());
                    output.accept(ModItems.BLIGHT_ARCHWOOD_LEAVES.get().getDefaultInstance());
                    output.accept(ModItems.STAFF_DISPLAY.get().getDefaultInstance());
                    output.accept(ModItems.BONE_CHEST.get().getDefaultInstance());
                    output.accept(ModItems.OSSUARY_BEACON.get().getDefaultInstance());
                    output.accept(ModFluids.BLIGHT_FLUID_BUCKET.get().getDefaultInstance());
                    output.accept(ModItems.MULTIPHASE_SPELL_TURRET.get().getDefaultInstance());
                    output.accept(ModItems.ACOLYTE_SPAWN_EGG.get().getDefaultInstance());
                    output.accept(ModItems.NECROMANCER_SPAWN_EGG.get().getDefaultInstance());
                    output.accept(ModItems.LICH_SPAWN_EGG.get().getDefaultInstance());
                    output.accept(ModItems.BONE_GOLEM_SPAWN_EGG.get().getDefaultInstance());
                    output.accept(ModItems.BLIGHT_VEIN.get().getDefaultInstance());
                    for (String name : ModBlocks.CORRUPTED_BASE_NAMES) {
                        output.accept(ModItems.CORRUPTED_BLOCK_ITEMS.get(name).get().getDefaultInstance());
                        output.accept(ModItems.CORRUPTED_STAIR_ITEMS.get(name).get().getDefaultInstance());
                        output.accept(ModItems.CORRUPTED_SLAB_ITEMS.get(name).get().getDefaultInstance());
                    }
                })
                .build();
    });
}
