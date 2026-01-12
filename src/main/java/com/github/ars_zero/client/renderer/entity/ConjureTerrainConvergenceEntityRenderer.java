package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.terrain.ConjureTerrainConvergenceEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ConjureTerrainConvergenceEntityRenderer
        extends AbstractGeometryEntityRenderer<ConjureTerrainConvergenceEntity> {

    public ConjureTerrainConvergenceEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new ConjureTerrainConvergenceEntityModel());
    }
}
