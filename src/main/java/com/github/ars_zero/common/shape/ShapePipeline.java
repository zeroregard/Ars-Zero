package com.github.ars_zero.common.shape;

import com.github.ars_zero.ArsZero;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class ShapePipeline {
    private static final Logger LOGGER = LogManager.getLogger(ArsZero.MOD_ID);
    private static final double SHELL_THICKNESS = 1.0;

    private ShapePipeline() {
    }

    public static List<BlockPos> generate(BlockPos center, int size, GeometryDescription description) {
        LOGGER.info("[ShapePipeline] Generating shape - description: {}, baseShape enum: {}, size: {}", 
            description, description.baseShape(), size);
        
        BaseShapeVolume baseShape = resolveBaseShape(description.baseShape());
        LOGGER.info("[ShapePipeline] Resolved baseShape class: {}", baseShape.getClass().getSimpleName());
        
        ShapeModifier modifier = description.isFlattened() ? FlattenModifier.INSTANCE : null;
        Vec3 orientation = description.orientation();
        boolean hollow = description.isHollow();

        double radius = size / 2.0;
        int halfSize = (int) Math.ceil(radius);

        List<BlockPos> result = new ArrayList<>();

        for (int dy = -halfSize; dy <= halfSize; dy++) {
            for (int dx = -halfSize; dx <= halfSize; dx++) {
                for (int dz = -halfSize; dz <= halfSize; dz++) {
                    if (shouldInclude(dx, dy, dz, radius, baseShape, modifier, orientation, hollow)) {
                        result.add(center.offset(dx, dy, dz));
                    }
                }
            }
        }

        LOGGER.info("[ShapePipeline] Generated {} block positions", result.size());
        return result;
    }

    private static boolean shouldInclude(int dx, int dy, int dz, double radius, 
            BaseShapeVolume baseShape, @Nullable ShapeModifier modifier, 
            @Nullable Vec3 orientation, boolean hollow) {
        
        double x = dx;
        double y = dy;
        double z = dz;

        if (modifier != null) {
            Vec3 transformed = modifier.transform(x, y, z, orientation);
            x = transformed.x;
            y = transformed.y;
            z = transformed.z;
        }

        if (hollow) {
            return baseShape.containsSurface(x, y, z, radius, SHELL_THICKNESS);
        } else {
            return baseShape.contains(x, y, z, radius);
        }
    }

    private static BaseShapeVolume resolveBaseShape(BaseShape shape) {
        return switch (shape) {
            case SPHERE -> SphereShape.INSTANCE;
            case CUBE -> CubeShape.INSTANCE;
        };
    }
}

