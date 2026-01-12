package com.github.ars_zero.common.shape;

public final class SphereShape implements BaseShapeVolume {
    public static final SphereShape INSTANCE = new SphereShape();

    private SphereShape() {
    }

    @Override
    public boolean contains(double x, double y, double z, double radius) {
        double distSq = x * x + y * y + z * z;
        return distSq <= radius * radius;
    }

    @Override
    public boolean containsSurface(double x, double y, double z, double radius, double thickness) {
        double distSq = x * x + y * y + z * z;
        double radiusSq = radius * radius;
        double innerRadius = Math.max(0, radius - thickness);
        double innerRadiusSq = innerRadius * innerRadius;
        return distSq <= radiusSq && distSq > innerRadiusSq;
    }
}


