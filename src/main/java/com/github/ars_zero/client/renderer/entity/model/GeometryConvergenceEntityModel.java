package com.github.ars_zero.client.renderer.entity.model;

import com.github.ars_zero.ArsZero;
import com.hollingsworth.arsnouveau.ArsNouveau;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/**
 * Shared golem model used by geometry convergence entities (terrain/break).
 */
public class GeometryConvergenceEntityModel<T extends Entity & GeoAnimatable> extends GeoModel<T> {
    private static final ResourceLocation MODEL = ArsNouveau.prefix("geo/amethyst_golem.geo.json");
    private static final ResourceLocation TEXTURE = ArsZero.prefix("textures/entity/terrain_golem.png");
    private static final ResourceLocation ANIMATION = ArsNouveau.prefix("animations/amethyst_golem_animations.json");

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }
}

