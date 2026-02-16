package com.github.ars_zero.client.renderer.item;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractStaff;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TelekinesisStaffModel extends GeoModel<AbstractStaff> {

    private static final ResourceLocation MODEL_RESOURCE = ArsZero.prefix("geo/telekinesis_staff.geo.json");
    private static final ResourceLocation TEXTURE_RESOURCE = ArsZero.prefix("textures/item/telekinesis_staff.png");
    private static final ResourceLocation ANIMATION_RESOURCE = ArsZero.prefix("animations/telekinesis_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(AbstractStaff object) {
        return MODEL_RESOURCE;
    }

    @Override
    public ResourceLocation getTextureResource(AbstractStaff object) {
        return TEXTURE_RESOURCE;
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractStaff animatable) {
        return ANIMATION_RESOURCE;
    }

    @Override
    public RenderType getRenderType(AbstractStaff animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
