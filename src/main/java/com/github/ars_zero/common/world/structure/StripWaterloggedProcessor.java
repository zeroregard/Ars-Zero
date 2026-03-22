package com.github.ars_zero.common.world.structure;

import com.github.ars_zero.registry.ModWorldgen;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StripWaterloggedProcessor extends StructureProcessor {

    public static final MapCodec<StripWaterloggedProcessor> CODEC = MapCodec.unit(new StripWaterloggedProcessor());

    @Override
    @Nullable
    public StructureTemplate.StructureBlockInfo processBlock(
            @Nonnull LevelReader level,
            @Nonnull BlockPos offset,
            @Nonnull BlockPos pos,
            @Nonnull StructureTemplate.StructureBlockInfo blockInfo,
            @Nonnull StructureTemplate.StructureBlockInfo relativeBlockInfo,
            @Nonnull StructurePlaceSettings settings) {

        BlockState state = relativeBlockInfo.state();

        // Force waterlogged=false if the block supports it and either the NBT says waterlogged
        // or the world position currently contains water (which would cause Minecraft to waterlog it).
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            boolean worldHasWater = level.getBlockState(pos).is(Blocks.WATER);
            if (worldHasWater || state.getValue(BlockStateProperties.WATERLOGGED)) {
                return new StructureTemplate.StructureBlockInfo(
                        relativeBlockInfo.pos(),
                        state.setValue(BlockStateProperties.WATERLOGGED, false),
                        relativeBlockInfo.nbt());
            }
        }
        return relativeBlockInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModWorldgen.STRIP_WATERLOGGED_PROCESSOR.get();
    }
}
