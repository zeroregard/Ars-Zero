package com.github.ars_zero.client.renderer.armor;

import com.github.ars_zero.common.item.armor.RottedArcanistArmor;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class RottedArcanistRenderer extends GeoArmorRenderer<RottedArcanistArmor> {

    public RottedArcanistRenderer() {
        super(new RottedArcanistModel());
    }
}
