package com.github.ars_zero.client.renderer.model;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.FilialItem;
import net.minecraft.resources.ResourceLocation;

public class StaticFilialGeoModel extends FilialGeoModel {

    @Override
    public ResourceLocation getAnimationResource(FilialItem item) {
        return ArsZero.prefix("animations/filial/static.animations.json");
    }
}
