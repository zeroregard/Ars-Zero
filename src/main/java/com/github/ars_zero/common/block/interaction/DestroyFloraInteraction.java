package com.github.ars_zero.common.block.interaction;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.block.BlightInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DestroyFloraInteraction implements BlightInteraction {
    private static final TagKey<net.minecraft.world.level.block.Block> BLIGHT_DESTROYABLE = 
        TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ArsZero.MOD_ID, "blight_destroyable"));
    
    @Override
    public boolean matches(BlockState state) {
        return state.is(BlockTags.LEAVES) || 
               state.is(BlockTags.FLOWERS) ||
               state.is(BlockTags.SAPLINGS) ||
               state.is(BLIGHT_DESTROYABLE) ||
               state.getBlock() == Blocks.SHORT_GRASS ||
               state.getBlock() == Blocks.TALL_GRASS ||
               state.getBlock() == Blocks.FERN ||
               state.getBlock() == Blocks.LARGE_FERN ||
               state.getBlock() == Blocks.DEAD_BUSH ||
               state.getBlock() == Blocks.VINE ||
               state.getBlock() == Blocks.GLOW_LICHEN;
    }
    
    @Override
    public void apply(ServerLevel level, BlockPos pos, BlockState state) {
        level.destroyBlock(pos, false);
    }
}
