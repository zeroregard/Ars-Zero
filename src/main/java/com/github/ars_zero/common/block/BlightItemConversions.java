package com.github.ars_zero.common.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

public final class BlightItemConversions {
    
    private static final Map<Block, Block> MOSSY_TO_CLEAN = Map.ofEntries(
        Map.entry(Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE),
        Map.entry(Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICKS),
        Map.entry(Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.COBBLESTONE_STAIRS),
        Map.entry(Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.COBBLESTONE_SLAB),
        Map.entry(Blocks.MOSSY_COBBLESTONE_WALL, Blocks.COBBLESTONE_WALL),
        Map.entry(Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.STONE_BRICK_STAIRS),
        Map.entry(Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.STONE_BRICK_SLAB),
        Map.entry(Blocks.MOSSY_STONE_BRICK_WALL, Blocks.STONE_BRICK_WALL),
        Map.entry(Blocks.MOSS_BLOCK, Blocks.COBBLESTONE)
    );
    
    private BlightItemConversions() {
    }
    
    public static Block convert(Block source) {
        Block mossy = MOSSY_TO_CLEAN.get(source);
        if (mossy != null) {
            return mossy;
        }
        if (source == Blocks.GRASS_BLOCK
            || source == Blocks.MYCELIUM
            || source == Blocks.PODZOL
            || source == Blocks.COARSE_DIRT
            || source == Blocks.FARMLAND) {
            return Blocks.DIRT;
        }
        return null;
    }
}


