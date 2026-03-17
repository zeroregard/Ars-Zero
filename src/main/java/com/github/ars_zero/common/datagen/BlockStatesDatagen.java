package com.github.ars_zero.common.datagen;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class BlockStatesDatagen extends BlockStateProvider {

    public BlockStatesDatagen(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ArsZero.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (String name : ModBlocks.CORRUPTED_BASE_NAMES) {
            ResourceLocation tex = ArsZero.prefix("block/" + name);

            simpleBlock(ModBlocks.CORRUPTED_BLOCKS.get(name).get(),
                    models().cubeAll(name, tex));

            stairsBlock((StairBlock) ModBlocks.CORRUPTED_STAIRS.get(name).get(), tex);

            slabBlock((SlabBlock) ModBlocks.CORRUPTED_SLABS.get(name).get(),
                    ArsZero.prefix("block/" + name), tex);
        }
    }
}
