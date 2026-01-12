package com.github.ars_zero.common.shape;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class GeometryDescription {
  public static final GeometryDescription DEFAULT = new GeometryDescription(
      BaseShape.CUBE, FillMode.SOLID, ProjectionMode.FULL_3D, null);

  private final BaseShape baseShape;
  private final FillMode fillMode;
  private final ProjectionMode projectionMode;
  @Nullable
  private final Vec3 orientation;

  public GeometryDescription(BaseShape baseShape, FillMode fillMode, ProjectionMode projectionMode,
      @Nullable Vec3 orientation) {
    this.baseShape = baseShape;
    this.fillMode = fillMode;
    this.projectionMode = projectionMode;
    this.orientation = orientation;
  }

  public BaseShape baseShape() {
    return baseShape;
  }

  public FillMode fillMode() {
    return fillMode;
  }

  public ProjectionMode projectionMode() {
    return projectionMode;
  }

  @Nullable
  public Vec3 orientation() {
    return orientation;
  }

  public boolean isFlattened() {
    return projectionMode == ProjectionMode.FLATTENED;
  }

  public boolean isHollow() {
    return fillMode == FillMode.HOLLOW;
  }

  public boolean isSphere() {
    return baseShape == BaseShape.SPHERE;
  }

  public GeometryDescription withOrientation(Vec3 newOrientation) {
    return new GeometryDescription(baseShape, fillMode, projectionMode, newOrientation);
  }

  public CompoundTag toTag() {
    CompoundTag tag = new CompoundTag();
    tag.putString("base_shape", baseShape.name());
    tag.putString("fill_mode", fillMode.name());
    tag.putString("projection_mode", projectionMode.name());
    if (orientation != null) {
      tag.putDouble("orientation_x", orientation.x);
      tag.putDouble("orientation_y", orientation.y);
      tag.putDouble("orientation_z", orientation.z);
    }
    return tag;
  }

  public static GeometryDescription fromTag(CompoundTag tag) {
    if (tag == null || tag.isEmpty()) {
      return DEFAULT;
    }

    BaseShape shape = BaseShape.CUBE;
    if (tag.contains("base_shape")) {
      try {
        shape = BaseShape.valueOf(tag.getString("base_shape"));
      } catch (IllegalArgumentException ignored) {
      }
    }

    FillMode fill = FillMode.SOLID;
    if (tag.contains("fill_mode")) {
      try {
        fill = FillMode.valueOf(tag.getString("fill_mode"));
      } catch (IllegalArgumentException ignored) {
      }
    }

    ProjectionMode projection = ProjectionMode.FULL_3D;
    if (tag.contains("projection_mode")) {
      try {
        projection = ProjectionMode.valueOf(tag.getString("projection_mode"));
      } catch (IllegalArgumentException ignored) {
      }
    }

    Vec3 orientation = null;
    if (tag.contains("orientation_x")) {
      orientation = new Vec3(
          tag.getDouble("orientation_x"),
          tag.getDouble("orientation_y"),
          tag.getDouble("orientation_z"));
    }

    return new GeometryDescription(shape, fill, projection, orientation);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(fillMode.name().toLowerCase());
    sb.append(" ");
    sb.append(baseShape.name().toLowerCase());
    if (projectionMode == ProjectionMode.FLATTENED) {
      sb.append(" (flattened)");
    }
    return sb.toString();
  }
}

