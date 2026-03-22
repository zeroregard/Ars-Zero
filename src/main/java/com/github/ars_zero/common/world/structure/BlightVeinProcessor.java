package com.github.ars_zero.common.world.structure;

import com.github.ars_zero.registry.ModBlocks;
import com.github.ars_zero.registry.ModWorldgen;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BlightVeinProcessor extends StructureProcessor {

    public static final float VEIN_CHANCE = 0.3f;
    public static final MapCodec<BlightVeinProcessor> CODEC = MapCodec.unit(new BlightVeinProcessor());

    @Override
    @Nonnull
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            @Nonnull ServerLevelAccessor level,
            @Nonnull BlockPos offset,
            @Nonnull BlockPos pos,
            @Nonnull List<StructureTemplate.StructureBlockInfo> originalBlockInfos,
            @Nonnull List<StructureTemplate.StructureBlockInfo> processedBlockInfos,
            @Nonnull StructurePlaceSettings settings) {

        Block excluded = ModBlocks.CORRUPTED_BLOCKS.get("smooth_corrupted_sourcestone").get();
        Set<Block> corruptedSet = ModBlocks.CORRUPTED_BLOCKS.values().stream()
                .map(h -> h.get())
                .filter(b -> b != excluded)
                .collect(Collectors.toSet());

        Map<BlockPos, BlockState> stateMap = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfos) {
            stateMap.put(info.pos(), info.state());
        }

        RandomSource rand = settings.getRandom(offset);
        Map<BlockPos, List<Direction>> veinFaces = new HashMap<>();

        for (Map.Entry<BlockPos, BlockState> entry : stateMap.entrySet()) {
            if (!corruptedSet.contains(entry.getValue().getBlock())) continue;
            BlockPos corruptedPos = entry.getKey();
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = corruptedPos.relative(dir);
                BlockState neighborState = stateMap.get(neighborPos);
                if (neighborState != null && neighborState.isAir()) {
                    if (rand.nextFloat() < VEIN_CHANCE) {
                        veinFaces.computeIfAbsent(neighborPos, k -> new ArrayList<>()).add(dir.getOpposite());
                    }
                }
            }
        }

        if (veinFaces.isEmpty()) return processedBlockInfos;

        // placeInWorld applies state.mirror(mirror).rotate(rotation) after finalizeProcessing.
        // Pre-invert that transform so the faces land in the correct world-space orientation.
        Rotation inverseRotation = switch (settings.getRotation()) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> settings.getRotation();
        };
        Mirror mirror = settings.getMirror();

        List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>(processedBlockInfos);
        for (Map.Entry<BlockPos, List<Direction>> entry : veinFaces.entrySet()) {
            BlockState veinState = ModBlocks.BLIGHT_VEIN.get().defaultBlockState();
            for (Direction face : entry.getValue()) {
                veinState = veinState.setValue(MultifaceBlock.getFaceProperty(face), true);
            }
            veinState = veinState.rotate(inverseRotation).mirror(mirror);
            result.add(new StructureTemplate.StructureBlockInfo(entry.getKey(), veinState, null));
        }
        return result;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModWorldgen.BLIGHT_VEIN_PROCESSOR.get();
    }
}
