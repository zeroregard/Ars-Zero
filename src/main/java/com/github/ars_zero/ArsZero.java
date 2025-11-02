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
import com.github.ars_zero.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ArsZero.MOD_ID)
public class ArsZero {
    public static final String MOD_ID = "ars_zero";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public ArsZero(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModGlyphs.registerGlyphs();
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        
        modEventBus.addListener(Networking::register);
        modEventBus.addListener(this::gatherData); 
        
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> {
            event.enqueueWork(() -> {
                ModItems.registerSpellCasters();
                registerVoxelInteractions();
            });
        });

        if (FMLEnvironment.dist.isClient()) {
            ArsZeroClient.init(modEventBus);
        }
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
            generator.addProvider(true, new com.github.ars_zero.common.datagen.StaffRecipeDatagen(generator));
        }
    }
}
