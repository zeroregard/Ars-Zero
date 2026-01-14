package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.client.renderer.entity.model.GeometryTerrainEntityModel;
import com.github.ars_zero.common.entity.terrain.GeometryTerrainEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class GeometryTerrainEntityRenderer
        extends AbstractGeometryEntityRenderer<GeometryTerrainEntity> {

    public GeometryTerrainEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new GeometryTerrainEntityModel());
    }
}
