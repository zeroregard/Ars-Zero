package com.github.ars_zero.client.renderer.item;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;

public class CreativeSpellStaffModel extends GeoModel<AbstractSpellStaff> {
    
    // Cache ResourceLocation objects to prevent creation every frame
    private static final ResourceLocation MODEL_RESOURCE = ArsZero.prefix("geo/creative_spell_staff.geo.json");
    private static final ResourceLocation TEXTURE_RESOURCE = ArsZero.prefix("textures/item/creative_spell_staff.png");
    private static final ResourceLocation ANIMATION_RESOURCE = ArsZero.prefix("animations/creative_spell_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(AbstractSpellStaff object) {
        return MODEL_RESOURCE;
    }

    @Override
    public ResourceLocation getTextureResource(AbstractSpellStaff object) {
        return TEXTURE_RESOURCE;
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractSpellStaff animatable) {
        return ANIMATION_RESOURCE;
    }

    @Override
    public RenderType getRenderType(AbstractSpellStaff animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
