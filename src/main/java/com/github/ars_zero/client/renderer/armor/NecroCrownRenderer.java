package com.github.ars_zero.client.renderer.armor;

import com.github.ars_zero.common.item.armor.NecroCrownArmor;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class NecroCrownRenderer extends GeoArmorRenderer<NecroCrownArmor> {

    public NecroCrownRenderer() {
        super(new NecroCrownModel());
    }
}
