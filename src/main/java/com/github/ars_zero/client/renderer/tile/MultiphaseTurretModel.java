package com.github.ars_zero.client.renderer.tile;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.block.MultiphaseSpellTurretTile;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class MultiphaseTurretModel extends GeoModel<MultiphaseSpellTurretTile> {
    
    private static final ResourceLocation MODEL_RESOURCE = ArsZero.prefix("geo/multiphase_turret.geo.json");
    private static final ResourceLocation TEXTURE_RESOURCE = com.hollingsworth.arsnouveau.ArsNouveau.prefix("textures/block/basic_spell_turret.png");
    private static final ResourceLocation ANIMATION_RESOURCE = ArsZero.prefix("animations/multi_phase_spell_turret.animation.json");

    @Override
    public ResourceLocation getModelResource(MultiphaseSpellTurretTile object) {
        return MODEL_RESOURCE;
    }

    @Override
    public ResourceLocation getTextureResource(MultiphaseSpellTurretTile object) {
        return TEXTURE_RESOURCE;
    }

    @Override
    public ResourceLocation getAnimationResource(MultiphaseSpellTurretTile animatable) {
        return ANIMATION_RESOURCE;
    }
}



