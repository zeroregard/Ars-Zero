package com.github.ars_zero.common.entity;

import com.github.ars_zero.common.shape.GeometryDescription;
import com.github.ars_zero.common.shape.ShapePipeline;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface IGeometryProcessEntity {

    enum BlockStatus {
        CLEAR,
        ADJACENT,
        BLOCKED
    }

    void setGeometryDescription(GeometryDescription description);

    GeometryDescription getGeometryDescription();

    int getSize();

    void setSize(int size);

    default int getDepth() {
        return 1;
    }

    default List<BlockPos> generatePositions(BlockPos center) {
        return ShapePipeline.generate(center, getSize(), getGeometryDescription(), getDepth());
    }

    void startProcess();

    boolean isProcessing();

    void cancelProcess();

    Level getProcessLevel();

    UUID getCasterUUID();

    @net.neoforged.api.distmarker.OnlyIn(Dist.CLIENT)
    default Map<BlockPos, BlockStatus> getBlockStatuses() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return Map.of();
        }

        Level level = getProcessLevel();
        if (level == null || !level.isClientSide) {
            return Map.of();
        }

        BlockPos center = BlockPos.containing(getEntityPosition());
        List<BlockPos> positions = generatePositions(center);
        Map<BlockPos, BlockStatus> result = new HashMap<>(positions.size());
        Set<BlockPos> positionSet = new HashSet<>(positions);

        for (BlockPos pos : positions) {
            BlockStatus status = computeStatusForBlock(level, pos, positionSet);
            result.put(pos, status);
        }

        return result;
    }

    @net.neoforged.api.distmarker.OnlyIn(Dist.CLIENT)
    default BlockStatus computeStatusForBlock(Level level, BlockPos pos, Set<BlockPos> shapePositions) {
        BlockState stateAtPos = level.getBlockState(pos);
        if (!stateAtPos.isAir() && !stateAtPos.canBeReplaced()) {
            return BlockStatus.BLOCKED;
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (shapePositions.contains(neighbor)) {
                continue;
            }
            BlockState neighborState = level.getBlockState(neighbor);
            if (!neighborState.isAir()) {
                return BlockStatus.ADJACENT;
            }
        }

        return BlockStatus.CLEAR;
    }

    @net.neoforged.api.distmarker.OnlyIn(Dist.CLIENT)
    default Vec3 getEntityPosition() {
        if (this instanceof Entity entity) {
            return entity.position();
        }
        return Vec3.ZERO;
    }
}
