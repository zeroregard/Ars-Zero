package com.github.ars_zero.client.renderer.model;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.block.tile.BoneChestBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BoneChestGeoModel extends GeoModel<BoneChestBlockEntity> {

    private static final ResourceLocation MODEL     = ArsZero.prefix("geo/bone_chest.geo.json");
    private static final ResourceLocation TEXTURE   = ArsZero.prefix("textures/entity/chest/bone_chest.png");
    private static final ResourceLocation ANIMATION = ArsZero.prefix("animations/bone_chest.animations.json");

    @Override
    public ResourceLocation getModelResource(BoneChestBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BoneChestBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BoneChestBlockEntity animatable) {
        return ANIMATION;
    }
}
