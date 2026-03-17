package com.github.ars_zero.common.world.structure;

import com.github.ars_zero.registry.ModWorldgen;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CobwebProcessor extends StructureProcessor {

    public static final float CHANCE = 0.3f;
    public static final MapCodec<CobwebProcessor> CODEC = MapCodec.unit(new CobwebProcessor());

    @Override
    @Nullable
    public StructureTemplate.StructureBlockInfo processBlock(
            @Nonnull LevelReader level,
            @Nonnull BlockPos offset,
            @Nonnull BlockPos pos,
            @Nonnull StructureTemplate.StructureBlockInfo blockInfo,
            @Nonnull StructureTemplate.StructureBlockInfo relativeBlockInfo,
            @Nonnull StructurePlaceSettings settings) {

        if (relativeBlockInfo.state().is(Blocks.COMMAND_BLOCK)) {
            if (settings.getRandom(relativeBlockInfo.pos()).nextFloat() < CHANCE) {
                return new StructureTemplate.StructureBlockInfo(
                        relativeBlockInfo.pos(),
                        Blocks.COBWEB.defaultBlockState(),
                        null);
            } else {
                return new StructureTemplate.StructureBlockInfo(
                        relativeBlockInfo.pos(),
                        Blocks.AIR.defaultBlockState(),
                        null);
            }
        }
        return relativeBlockInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModWorldgen.COBWEB_PROCESSOR.get();
    }
}
