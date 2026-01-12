package com.github.ars_zero.common.entity;

import com.github.ars_zero.common.shape.GeometryDescription;
import com.github.ars_zero.common.shape.ShapePipeline;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

public interface IGeometryProcessEntity {

    void setGeometryDescription(GeometryDescription description);

    GeometryDescription getGeometryDescription();

    int getSize();

    void setSize(int size);

    default List<BlockPos> generatePositions(BlockPos center) {
        return ShapePipeline.generate(center, getSize(), getGeometryDescription());
    }

    void startProcess();

    boolean isProcessing();

    void cancelProcess();

    Level getProcessLevel();
}


