package com.github.ars_zero.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlightVeinBlock extends MultifaceBlock {

    private static final MapCodec<BlightVeinBlock> CODEC = simpleCodec(BlightVeinBlock::new);
    private final MultifaceSpreader spreader = new MultifaceSpreader(this);

    public BlightVeinBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BlightVeinBlock> codec() {
        return CODEC;
    }

    @Override
    public MultifaceSpreader getSpreader() {
        return this.spreader;
    }
}
