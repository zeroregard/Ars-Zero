package com.github.ars_zero.common.shape;

public final class CubeShape implements BaseShapeVolume {
    public static final CubeShape INSTANCE = new CubeShape();

    private CubeShape() {
    }

    @Override
    public boolean contains(double x, double y, double z, double radius) {
        return Math.abs(x) <= radius && Math.abs(y) <= radius && Math.abs(z) <= radius;
    }

    @Override
    public boolean containsSurface(double x, double y, double z, double radius, double thickness) {
        if (!contains(x, y, z, radius)) {
            return false;
        }
        double innerRadius = Math.max(0, radius - thickness);
        boolean insideInner = Math.abs(x) <= innerRadius 
                && Math.abs(y) <= innerRadius 
                && Math.abs(z) <= innerRadius;
        return !insideInner;
    }
}


