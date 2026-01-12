package com.github.ars_zero.common.shape;

import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public interface ShapeModifier {

  Vec3 transform(double x, double y, double z, @Nullable Vec3 orientation);

  default double transformRadius(double radius) {
    return radius;
  }
}


