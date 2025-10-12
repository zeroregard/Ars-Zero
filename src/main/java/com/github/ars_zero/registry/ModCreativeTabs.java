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
                    return ModItems.ARS_ZERO_STAFF.get().getDefaultInstance();
                })
                .displayItems((params, output) -> {
                    ArsZero.LOGGER.debug("Populating Ars Zero creative tab with items");
                    output.accept(ModItems.ARS_ZERO_STAFF.get().getDefaultInstance());
                })
                .build();
    });
}
