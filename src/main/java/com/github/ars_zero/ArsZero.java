package com.github.ars_zero;

import com.github.ars_zero.client.ArsZeroClient;
import com.github.ars_zero.common.entity.ArcaneVoxelEntity;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.common.entity.interaction.ArcaneCollisionInteraction;
import com.github.ars_zero.common.entity.interaction.FireWaterInteraction;
import com.github.ars_zero.common.entity.interaction.MergeInteraction;
import com.github.ars_zero.common.entity.interaction.VoxelInteractionRegistry;
import com.github.ars_zero.common.network.Networking;
import com.github.ars_zero.registry.ModAttachments;
import com.github.ars_zero.registry.ModBlockEntities;
import com.github.ars_zero.registry.ModBlocks;
import com.github.ars_zero.registry.ModCreativeTabs;
import com.github.ars_zero.registry.ModEntities;
import com.github.ars_zero.registry.ModItems;
import com.github.ars_zero.registry.ModGlyphs;
import com.github.ars_zero.registry.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
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

        LOGGER.debug("Registering blocks...");
        ModBlocks.BLOCKS.register(modEventBus);
        LOGGER.info("Registered {} blocks", ModBlocks.BLOCKS.getEntries().size());
        
        LOGGER.debug("Registering block entities...");
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        LOGGER.info("Registered {} block entities", ModBlockEntities.BLOCK_ENTITIES.getEntries().size());
        
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
        
        LOGGER.debug("Registering attachment types...");
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        LOGGER.info("Registered {} attachment types", ModAttachments.ATTACHMENT_TYPES.getEntries().size());
        
        LOGGER.debug("Registering recipe types and serializers...");
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        LOGGER.info("Registered recipe types and serializers");
        
        modEventBus.addListener(Networking::register);
        modEventBus.addListener(this::gatherData); 
        
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> {
            event.enqueueWork(() -> {
                LOGGER.info("Registering spell casters with Ars Nouveau...");
                ModItems.registerSpellCasters();
                LOGGER.info("Spell caster registration completed");
                
                LOGGER.info("Registering voxel interactions...");
                registerVoxelInteractions();
                LOGGER.info("Voxel interaction registration completed");
            });
        });

        if (FMLEnvironment.dist.isClient()) {
            LOGGER.debug("Initializing client-side components...");
            ArsZeroClient.init(modEventBus);
            LOGGER.info("Client initialization completed");
        } else {
            LOGGER.debug("Skipping client initialization (server-side)");
        }

        LOGGER.info("Ars Zero mod initialization completed successfully!");
    }

    private static void registerVoxelInteractions() {
        MergeInteraction mergeInteraction = new MergeInteraction();
        ArcaneCollisionInteraction arcaneInteraction = new ArcaneCollisionInteraction();
        
        VoxelInteractionRegistry.register(
            FireVoxelEntity.class,
            WaterVoxelEntity.class,
            new FireWaterInteraction()
        );
        
        VoxelInteractionRegistry.register(
            FireVoxelEntity.class,
            FireVoxelEntity.class,
            mergeInteraction
        );
        
        VoxelInteractionRegistry.register(
            WaterVoxelEntity.class,
            WaterVoxelEntity.class,
            mergeInteraction
        );
        
        VoxelInteractionRegistry.register(
            ArcaneVoxelEntity.class,
            ArcaneVoxelEntity.class,
            arcaneInteraction
        );
        
        VoxelInteractionRegistry.register(
            ArcaneVoxelEntity.class,
            FireVoxelEntity.class,
            arcaneInteraction
        );
        
        VoxelInteractionRegistry.register(
            ArcaneVoxelEntity.class,
            WaterVoxelEntity.class,
            arcaneInteraction
        );
    }
    
    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
    
    public void gatherData(net.neoforged.neoforge.data.event.GatherDataEvent event) {
        var generator = event.getGenerator();
        
        if (event.includeServer()) {
            generator.addProvider(true, new com.github.ars_zero.common.datagen.DyeRecipeDatagen(generator));
        }
    }
}
