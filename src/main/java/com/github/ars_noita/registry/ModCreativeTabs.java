package com.github.ars_noita.registry;

import com.github.ars_noita.ArsNoita;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArsNoita.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARS_NOITA_TAB = TABS.register("general", () -> {
        ArsNoita.LOGGER.debug("Creating Ars Noita creative tab");
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.ars_noita"))
                .icon(() -> {
                    ArsNoita.LOGGER.debug("Getting icon for Ars Noita creative tab");
                    return ModItems.ARS_NOITA_STAFF.get().getDefaultInstance();
                })
                .displayItems((params, output) -> {
                    ArsNoita.LOGGER.debug("Populating Ars Noita creative tab with items");
                    output.accept(ModItems.ARS_NOITA_STAFF.get().getDefaultInstance());
                })
                .build();
    });
}
