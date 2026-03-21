package com.github.ars_zero.client.renderer.armor;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.armor.TatteredArcanistArmor;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TatteredArcanistModel extends GeoModel<TatteredArcanistArmor> {

    @Override
    public ResourceLocation getModelResource(TatteredArcanistArmor animatable) {
        return ArsZero.prefix("geo/tattered_arcanist.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TatteredArcanistArmor animatable) {
        return ArsZero.prefix("textures/armor/tattered_arcanist_black.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TatteredArcanistArmor animatable) {
        return ArsZero.prefix("animations/empty.json");
    }
}
