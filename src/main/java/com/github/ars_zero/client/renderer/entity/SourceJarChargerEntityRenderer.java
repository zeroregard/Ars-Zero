package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.SourceJarChargerEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class SourceJarChargerEntityRenderer extends EntityRenderer<SourceJarChargerEntity> {
    
    private static final ResourceLocation TEXTURE = ArsZero.prefix("textures/entity/empty.png");
    
    public SourceJarChargerEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SourceJarChargerEntity entity) {
        return TEXTURE;
    }
}

