package com.github.ars_zero.client.renderer.armor;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.armor.NecroCrownArmor;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NecroCrownModel extends GeoModel<NecroCrownArmor> {

    @Override
    public ResourceLocation getModelResource(NecroCrownArmor animatable) {
        return ArsZero.prefix("geo/necro_crown.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NecroCrownArmor animatable) {
        return ArsZero.prefix("textures/armor/necro_crown.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NecroCrownArmor animatable) {
        return ArsZero.prefix("animations/empty.json");
    }
}
