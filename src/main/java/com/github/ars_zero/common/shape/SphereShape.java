package com.github.ars_zero.common.shape;

public final class SphereShape implements BaseShapeVolume {
    public static final SphereShape INSTANCE = new SphereShape();

    private static final double HALF_BLOCK = 0.5;

    private SphereShape() {
    }

    @Override
    public boolean contains(double x, double y, double z, double radius) {
        double closestX = x > 0 ? Math.max(0, x - HALF_BLOCK) : Math.min(0, x + HALF_BLOCK);
        double closestY = y > 0 ? Math.max(0, y - HALF_BLOCK) : Math.min(0, y + HALF_BLOCK);
        double closestZ = z > 0 ? Math.max(0, z - HALF_BLOCK) : Math.min(0, z + HALF_BLOCK);

        double distSq = closestX * closestX + closestY * closestY + closestZ * closestZ;
        return distSq <= radius * radius;
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
