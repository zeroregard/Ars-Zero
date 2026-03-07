package com.github.ars_zero.client.renderer.armor;

import com.github.ars_zero.common.item.armor.TatteredArcanistArmor;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class TatteredArcanistRenderer extends GeoArmorRenderer<TatteredArcanistArmor> {

    public TatteredArcanistRenderer() {
        super(new TatteredArcanistModel());
    }
}
