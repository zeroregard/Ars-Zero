package com.github.ars_zero.common.world.structure;

import com.github.ars_zero.registry.ModWorldgen;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;

public class RandomBlockSubsetProcessor extends StructureProcessor {

    public static final MapCodec<RandomBlockSubsetProcessor> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        BuiltInRegistries.BLOCK.byNameCodec().fieldOf("target_block").forGetter(p -> p.targetBlock),
        Codec.INT.listOf().fieldOf("count_weights").forGetter(p -> p.countWeights)
    ).apply(inst, RandomBlockSubsetProcessor::new));

    private final Block targetBlock;
    private final List<Integer> countWeights;

    public RandomBlockSubsetProcessor(Block targetBlock, List<Integer> countWeights) {
        this.targetBlock = targetBlock;
        this.countWeights = countWeights;
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            ServerLevelAccessor level, BlockPos offset, BlockPos pos,
            List<StructureTemplate.StructureBlockInfo> originalBlockInfos,
            List<StructureTemplate.StructureBlockInfo> processedBlockInfos,
            StructurePlaceSettings settings) {

        List<Integer> targetIndices = new ArrayList<>();
        for (int i = 0; i < processedBlockInfos.size(); i++) {
            if (processedBlockInfos.get(i).state().is(targetBlock)) {
                targetIndices.add(i);
            }
        }
        if (targetIndices.isEmpty()) return processedBlockInfos;

        RandomSource rand = settings.getRandom(offset);
        int n = drawWeighted(rand, Math.min(countWeights.size() - 1, targetIndices.size()));

        // Fisher-Yates partial shuffle — move n random elements to the front
        for (int i = 0; i < n; i++) {
            int swapWith = i + rand.nextInt(targetIndices.size() - i);
            int tmp = targetIndices.get(i);
            targetIndices.set(i, targetIndices.get(swapWith));
            targetIndices.set(swapWith, tmp);
        }

        // Replace indices [n, size) with air
        List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>(processedBlockInfos);
        for (int i = n; i < targetIndices.size(); i++) {
            int idx = targetIndices.get(i);
            StructureTemplate.StructureBlockInfo original = result.get(idx);
            result.set(idx, new StructureTemplate.StructureBlockInfo(
                original.pos(), Blocks.AIR.defaultBlockState(), null));
        }
        return result;
    }

    private int drawWeighted(RandomSource rand, int maxCount) {
        int total = 0;
        for (int i = 0; i <= maxCount; i++) {
            if (i < countWeights.size()) total += countWeights.get(i);
        }
        if (total <= 0) return maxCount;
        int roll = rand.nextInt(total);
        int cumulative = 0;
        for (int i = 0; i <= maxCount; i++) {
            if (i < countWeights.size()) cumulative += countWeights.get(i);
            if (roll < cumulative) return i;
        }
        return maxCount;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModWorldgen.RANDOM_BLOCK_SUBSET_PROCESSOR.get();
    }
}
