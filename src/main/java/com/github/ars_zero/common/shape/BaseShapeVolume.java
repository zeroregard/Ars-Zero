package com.github.ars_zero.common.shape;

public interface BaseShapeVolume {

    boolean contains(double x, double y, double z, double radius);

    boolean containsSurface(double x, double y, double z, double radius, double thickness);
}



