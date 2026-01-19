package com.github.ars_zero.common.shape;

public final class SphereShape implements BaseShapeVolume {
    public static final SphereShape INSTANCE = new SphereShape();

    private SphereShape() {
    }

    @Override
    public boolean contains(double x, double y, double z, double radius) {
        double distSq = x * x + y * y + z * z;
        return distSq < radius * radius;
    }

    @Override
    public boolean containsSurface(double x, double y, double z, double radius, double thickness) {
        if (!contains(x, y, z, radius)) {
            return false;
        }

        boolean xPlusOutside = !contains(x + 1, y, z, radius);
        boolean xMinusOutside = !contains(x - 1, y, z, radius);
        boolean yPlusOutside = !contains(x, y + 1, z, radius);
        boolean yMinusOutside = !contains(x, y - 1, z, radius);
        boolean zPlusOutside = !contains(x, y, z + 1, radius);
        boolean zMinusOutside = !contains(x, y, z - 1, radius);

        return xPlusOutside || xMinusOutside || yPlusOutside || yMinusOutside || zPlusOutside || zMinusOutside;
    }
}
