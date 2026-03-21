package com.github.ars_zero.client.renderer.armor;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.armor.RottedArcanistArmor;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RottedArcanistModel extends GeoModel<RottedArcanistArmor> {

    @Override
    public ResourceLocation getModelResource(RottedArcanistArmor animatable) {
        return ArsZero.prefix("geo/tattered_arcanist.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RottedArcanistArmor animatable) {
        return ArsZero.prefix("textures/armor/rotten_arcanist_black.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RottedArcanistArmor animatable) {
        return ArsZero.prefix("animations/empty.json");
    }
}
