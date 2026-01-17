package com.github.ars_zero.common.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class ShapePipeline {
    private static final double SHELL_THICKNESS = 1.0;

    private ShapePipeline() {
    }

    public static List<BlockPos> generate(BlockPos center, int size, GeometryDescription description) {
        return generate(center, size, description, 1);
    }

    public static List<BlockPos> generate(BlockPos center, int size, GeometryDescription description, int depth) {
        BaseShapeVolume baseShape = resolveBaseShape(description.baseShape());
        ShapeModifier modifier = description.isFlattened() ? FlattenModifier.INSTANCE : null;
        Vec3 orientation = description.orientation();
        boolean hollow = description.isHollow();

        boolean isEvenSize = (size % 2) == 0;
        double evenOffset = isEvenSize ? 0.5 : 0.0;
        double radius = size / 2.0;

        int halfSize = (int) Math.ceil(size / 2.0);

        List<BlockPos> result = new ArrayList<>();

        // Determine which axis to flatten if using flatten modifier
        FlattenModifier.Axis flatAxis = null;
        if (description.isFlattened()) {
            flatAxis = FlattenModifier.determineFlatAxis(orientation);
        }

        // Extend iteration ranges to account for depth
        int minX = -halfSize, maxX = halfSize;
        int minY = -halfSize, maxY = halfSize;
        int minZ = -halfSize, maxZ = halfSize;

        if (flatAxis != null) {
            int depthMin = depth >= 0 ? 0 : depth;
            int depthMax = depth >= 0 ? depth - 1 : -1;
            switch (flatAxis) {
                case X -> {
                    minX = Math.min(minX, depthMin);
                    maxX = Math.max(maxX, depthMax);
                }
                case Y -> {
                    minY = Math.min(minY, depthMin);
                    maxY = Math.max(maxY, depthMax);
                }
                case Z -> {
                    minZ = Math.min(minZ, depthMin);
                    maxZ = Math.max(maxZ, depthMax);
                }
            }
        }

        for (int dy = minY; dy <= maxY; dy++) {
            for (int dx = minX; dx <= maxX; dx++) {
                for (int dz = minZ; dz <= maxZ; dz++) {
                    // If flattening, only include blocks within the depth range of the flat axis
                    if (flatAxis != null) {
                        int depthStart = depth >= 0 ? 0 : depth;
                        int depthEnd = depth >= 0 ? depth - 1 : -1;

                        boolean skipBlock = switch (flatAxis) {
                            case X -> dx < depthStart || dx > depthEnd;
                            case Y -> dy < depthStart || dy > depthEnd;
                            case Z -> dz < depthStart || dz > depthEnd;
                        };
                        if (skipBlock) {
                            continue;
                        }
                    }

                    double sampleX = dx + evenOffset;
                    double sampleY = dy + evenOffset;
                    double sampleZ = dz + evenOffset;

                    if (shouldInclude(sampleX, sampleY, sampleZ, radius, baseShape, modifier, orientation, hollow)) {
                        result.add(center.offset(dx, dy, dz));
                    }
                }
            }
        }

        return result;
    }

    private static boolean shouldInclude(double dx, double dy, double dz, double radius,
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
            if (modifier instanceof FlattenModifier) {
                return is2DCircleEdge(x, y, z, radius);
            }
            return baseShape.containsSurface(x, y, z, radius, SHELL_THICKNESS);
        } else {
            return baseShape.contains(x, y, z, radius);
        }
    }

    private static boolean is2DCircleEdge(double x, double y, double z, double radius) {
        double centerDist = Math.sqrt(x * x + y * y + z * z);
        double minCornerDist = Math.sqrt(
                Math.max(0, Math.abs(x) - 0.5) * Math.max(0, Math.abs(x) - 0.5) +
                        Math.max(0, Math.abs(y) - 0.5) * Math.max(0, Math.abs(y) - 0.5) +
                        Math.max(0, Math.abs(z) - 0.5) * Math.max(0, Math.abs(z) - 0.5));

        double outerBound = radius + 0.5;
        double innerBound = radius - 0.5;

        return minCornerDist <= radius && centerDist >= innerBound;
    }

    private static BaseShapeVolume resolveBaseShape(BaseShape shape) {
        return switch (shape) {
            case SPHERE -> SphereShape.INSTANCE;
            case CUBE -> CubeShape.INSTANCE;
        };
    }
}
