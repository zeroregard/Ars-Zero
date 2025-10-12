package com.github.ars_zero;

import com.github.ars_zero.client.ArsZeroClient;
import com.github.ars_zero.event.ArsZeroStaffEvents;
import com.github.ars_zero.registry.ModCreativeTabs;
import com.github.ars_zero.registry.ModEntities;
import com.github.ars_zero.registry.ModItems;
import com.github.ars_zero.registry.ModGlyphs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Logger;

@Mod(ArsZero.MOD_ID)
public class ArsZero {
    public static final String MOD_ID = "ars_zero";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    static {
        if (!FMLEnvironment.production) {
            Configurator.setLevel(LOGGER.getName(), org.apache.logging.log4j.Level.DEBUG);
        } else {
            Configurator.setLevel(LOGGER.getName(), org.apache.logging.log4j.Level.WARN);
        }
    }

    public ArsZero(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Ars Zero mod...");
        LOGGER.debug("Mod container: {}", modContainer.getModId());
        LOGGER.debug("Environment: {}", FMLEnvironment.dist);

        LOGGER.debug("Registering entities...");
        ModEntities.ENTITIES.register(modEventBus);
        LOGGER.info("Registered {} entities", ModEntities.ENTITIES.getEntries().size());

        LOGGER.debug("Registering items...");
        ModItems.ITEMS.register(modEventBus);
        LOGGER.info("Registered {} items", ModItems.ITEMS.getEntries().size());

        LOGGER.debug("Registering creative tabs...");
        ModCreativeTabs.TABS.register(modEventBus);
        LOGGER.info("Registered {} creative tabs", ModCreativeTabs.TABS.getEntries().size());

        LOGGER.debug("Registering glyphs using Ars Nouveau's system...");
        ModGlyphs.registerGlyphs();
        LOGGER.info("Glyph registration completed");

        // Register spell casters after a short delay to ensure items are fully registered
        if (FMLEnvironment.dist.isClient()) {
            LOGGER.debug("Scheduling spell caster registration...");
            // Use a timer to register spell casters after items are fully loaded
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    LOGGER.info("Registering spell casters with Ars Nouveau...");
                    ModItems.registerSpellCasters();
                    LOGGER.info("Spell caster registration completed");
                }
            }, 1000); // 1 second delay
        }

        if (FMLEnvironment.dist.isClient()) {
            LOGGER.debug("Initializing client-side components...");
            ArsZeroClient.init(modEventBus);
            LOGGER.info("Client initialization completed");
        } else {
            LOGGER.debug("Skipping client initialization (server-side)");
        }

        // Register event handlers
        LOGGER.debug("Registering event handlers...");
        NeoForge.EVENT_BUS.register(ArsZeroStaffEvents.class);
        LOGGER.info("Event handlers registered");

        LOGGER.info("Ars Noita mod initialization completed successfully!");
    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
