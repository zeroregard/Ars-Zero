package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.client.renderer.entity.model.GeometryBreakEntityModel;
import com.github.ars_zero.common.entity.break_blocks.GeometryBreakEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class GeometryBreakEntityRenderer extends AbstractGeometryEntityRenderer<GeometryBreakEntity> {

  public GeometryBreakEntityRenderer(EntityRendererProvider.Context context) {
    super(context, new GeometryBreakEntityModel());
  }
}
