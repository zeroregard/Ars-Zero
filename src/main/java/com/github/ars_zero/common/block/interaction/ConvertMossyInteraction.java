package com.github.ars_zero.common.block.interaction;

import com.github.ars_zero.common.block.BlightLiquidBlock;
import com.github.ars_zero.common.block.BlightInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class ConvertMossyInteraction implements BlightInteraction {
    private static final Map<Block, Block> MOSSY_MAP = Map.ofEntries(
        Map.entry(Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE),
        Map.entry(Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICKS),
        Map.entry(Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.COBBLESTONE_STAIRS),
        Map.entry(Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.COBBLESTONE_SLAB),
        Map.entry(Blocks.MOSSY_COBBLESTONE_WALL, Blocks.COBBLESTONE_WALL),
        Map.entry(Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.STONE_BRICK_STAIRS),
        Map.entry(Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.STONE_BRICK_SLAB),
        Map.entry(Blocks.MOSSY_STONE_BRICK_WALL, Blocks.STONE_BRICK_WALL)
    );
    
    @Override
    public boolean matches(BlockState state) {
        return MOSSY_MAP.containsKey(state.getBlock());
    }
    
    @Override
    public void apply(ServerLevel level, BlockPos pos, BlockState oldState) {
        Block target = MOSSY_MAP.get(oldState.getBlock());
        if (target != null) {
            BlockState newState = BlightLiquidBlock.copyProperties(oldState, target.defaultBlockState());
            level.setBlock(pos, newState, 3);
        }
    }
}
