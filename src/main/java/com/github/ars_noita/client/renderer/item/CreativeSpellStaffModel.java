package com.github.ars_noita.client.renderer.item;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.common.item.ArsNoitaStaff;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;

public class CreativeSpellStaffModel extends GeoModel<ArsNoitaStaff> {

    @Override
    public ResourceLocation getModelResource(ArsNoitaStaff object) {
        return ArsNoita.prefix("geo/creative_spell_staff.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ArsNoitaStaff object) {
        return ArsNoita.prefix("textures/item/creative_spell_staff.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ArsNoitaStaff animatable) {
        return ArsNoita.prefix("animations/creative_spell_staff.animation.json");
    }

    @Override
    public RenderType getRenderType(ArsNoitaStaff animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
