package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.explosion.ExplosionBurstProjectile;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ExplosionBurstProjectileRenderer extends EntityRenderer<ExplosionBurstProjectile> {
    
    private static final ResourceLocation TEXTURE = ArsZero.prefix("textures/entity/empty.png");
    
    public ExplosionBurstProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public ResourceLocation getTextureLocation(ExplosionBurstProjectile entity) {
        return TEXTURE;
    }
}

