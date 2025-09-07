package com.github.ars_noita;

import com.github.ars_noita.client.ArsNoitaClient;
import com.github.ars_noita.registry.ModCreativeTabs;
import com.github.ars_noita.registry.ModItems;
import com.github.ars_noita.registry.ModGlyphs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ArsNoita.MOD_ID)
public class ArsNoita {
    public static final String MOD_ID = "ars_noita";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public ArsNoita(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        
        // Register glyphs using Ars Nouveau's system
        ModGlyphs.registerGlyphs();
        
        if (FMLEnvironment.dist.isClient()) {
            ArsNoitaClient.init(modEventBus);
        }
    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
