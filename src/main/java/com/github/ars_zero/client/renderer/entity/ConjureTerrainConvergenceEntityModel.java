package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.terrain.ConjureTerrainConvergenceEntity;
import com.hollingsworth.arsnouveau.ArsNouveau;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ConjureTerrainConvergenceEntityModel extends GeoModel<ConjureTerrainConvergenceEntity> {

    private static final ResourceLocation MODEL = ArsNouveau.prefix("geo/amethyst_golem.geo.json");
    private static final ResourceLocation TEXTURE = ArsZero.prefix("textures/entity/terrain_golem.png");
    private static final ResourceLocation ANIMATIONS = ArsNouveau.prefix("animations/amethyst_golem_animations.json");

    @Override
    public ResourceLocation getModelResource(ConjureTerrainConvergenceEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ConjureTerrainConvergenceEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ConjureTerrainConvergenceEntity entity) {
        return ANIMATIONS;
    }
}

