package com.github.ars_zero.client.renderer.model;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.FilialItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public abstract class FilialGeoModel extends GeoModel<FilialItem> {

    @Override
    public ResourceLocation getModelResource(FilialItem item) {
        return ArsZero.prefix("geo/filial/" + item.getSchoolId() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FilialItem item) {
        return ArsZero.prefix("textures/item/filial/" + item.getSchoolId() + ".png");
    }
}
