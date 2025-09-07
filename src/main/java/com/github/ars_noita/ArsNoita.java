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
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Logger;

@Mod(ArsNoita.MOD_ID)
public class ArsNoita {
    public static final String MOD_ID = "ars_noita";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    static {
        if (!FMLEnvironment.production) {
            Configurator.setLevel(LOGGER.getName(), org.apache.logging.log4j.Level.DEBUG);
        } else {
            Configurator.setLevel(LOGGER.getName(), org.apache.logging.log4j.Level.WARN);
        }
    }

    public ArsNoita(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Ars Noita mod...");
        LOGGER.debug("Mod container: {}", modContainer.getModId());
        LOGGER.debug("Environment: {}", FMLEnvironment.dist);

        LOGGER.debug("Registering items...");
        ModItems.ITEMS.register(modEventBus);
        LOGGER.info("Registered {} items", ModItems.ITEMS.getEntries().size());

        LOGGER.debug("Registering creative tabs...");
        ModCreativeTabs.TABS.register(modEventBus);
        LOGGER.info("Registered {} creative tabs", ModCreativeTabs.TABS.getEntries().size());

        LOGGER.debug("Registering glyphs using Ars Nouveau's system...");
        ModGlyphs.registerGlyphs();
        LOGGER.info("Glyph registration completed");

        if (FMLEnvironment.dist.isClient()) {
            LOGGER.debug("Initializing client-side components...");
            ArsNoitaClient.init(modEventBus);
            LOGGER.info("Client initialization completed");
        } else {
            LOGGER.debug("Skipping client initialization (server-side)");
        }

        LOGGER.info("Ars Noita mod initialization completed successfully!");
    }

    public static ResourceLocation prefix(String path) {
        LOGGER.debug("Creating resource location: {}:{}", MOD_ID, path);
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
