package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.PlayerChargerEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class PlayerChargerEntityRenderer extends EntityRenderer<PlayerChargerEntity> {
    
    private static final ResourceLocation TEXTURE = ArsZero.prefix("textures/entity/empty.png");
    
    public PlayerChargerEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerChargerEntity entity) {
        return TEXTURE;
    }
}

