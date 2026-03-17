package com.github.ars_zero.common.datagen;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class BlockTagDatagen extends BlockTagsProvider {

    public BlockTagDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ArsZero.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (String name : ModBlocks.CORRUPTED_BASE_NAMES) {
            Block base = ModBlocks.CORRUPTED_BLOCKS.get(name).get();
            Block stair = ModBlocks.CORRUPTED_STAIRS.get(name).get();
            Block slab = ModBlocks.CORRUPTED_SLABS.get(name).get();

            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(base, stair, slab);
            tag(BlockTags.STAIRS).add(stair);
            tag(BlockTags.SLABS).add(slab);
        }
    }
}
