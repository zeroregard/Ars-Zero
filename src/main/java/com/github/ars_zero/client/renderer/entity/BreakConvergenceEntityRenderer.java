package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.client.renderer.entity.model.BreakConvergenceEntityModel;
import com.github.ars_zero.common.entity.break_blocks.BreakConvergenceEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class BreakConvergenceEntityRenderer extends AbstractGeometryEntityRenderer<BreakConvergenceEntity> {

  public BreakConvergenceEntityRenderer(EntityRendererProvider.Context context) {
    super(context, new BreakConvergenceEntityModel());
  }
}
