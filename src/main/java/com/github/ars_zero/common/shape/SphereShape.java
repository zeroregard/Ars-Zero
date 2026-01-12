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
        double closestX = x > 0 ? Math.max(0, x - HALF_BLOCK) : Math.min(0, x + HALF_BLOCK);
        double closestY = y > 0 ? Math.max(0, y - HALF_BLOCK) : Math.min(0, y + HALF_BLOCK);
        double closestZ = z > 0 ? Math.max(0, z - HALF_BLOCK) : Math.min(0, z + HALF_BLOCK);

        double closestDistSq = closestX * closestX + closestY * closestY + closestZ * closestZ;

        double farthestX = x < 0 ? x - HALF_BLOCK : x + HALF_BLOCK;
        double farthestY = y < 0 ? y - HALF_BLOCK : y + HALF_BLOCK;
        double farthestZ = z < 0 ? z - HALF_BLOCK : z + HALF_BLOCK;

        double farthestDistSq = farthestX * farthestX + farthestY * farthestY + farthestZ * farthestZ;

        double radiusSq = radius * radius;
        double innerRadius = Math.max(0, radius - thickness);
        double innerRadiusSq = innerRadius * innerRadius;

        return farthestDistSq >= innerRadiusSq && closestDistSq <= radiusSq;
    }
}
