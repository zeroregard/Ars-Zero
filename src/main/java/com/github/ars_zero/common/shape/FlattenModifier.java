package com.github.ars_zero.common.shape;

import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class FlattenModifier implements ShapeModifier {
    public static final FlattenModifier INSTANCE = new FlattenModifier();

    private FlattenModifier() {
    }

    @Override
    public Vec3 transform(double x, double y, double z, @Nullable Vec3 orientation) {
        Axis flatAxis = determineFlatAxis(orientation);
        return switch (flatAxis) {
            case X -> new Vec3(0, y, z);
            case Y -> new Vec3(x, 0, z);
            case Z -> new Vec3(x, y, 0);
        };
    }

    public static Axis determineFlatAxis(@Nullable Vec3 orientation) {
        if (orientation == null) {
            return Axis.Y;
        }

        double absX = Math.abs(orientation.x);
        double absY = Math.abs(orientation.y);
        double absZ = Math.abs(orientation.z);

        if (absY >= absX && absY >= absZ) {
            return Axis.Y;
        } else if (absX >= absZ) {
            return Axis.X;
        } else {
            return Axis.Z;
        }
    }

    public enum Axis {
        X, Y, Z
    }
}


