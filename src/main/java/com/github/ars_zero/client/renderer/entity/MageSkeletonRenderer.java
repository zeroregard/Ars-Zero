package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;

/**
 * Skeleton renderer for the Mage Skeleton, adding an enchanted glint layer on the skeleton body only.
 * Uses the custom blighted_skeleton.png texture (vanilla skeleton base).
 */
public class MageSkeletonRenderer extends SkeletonRenderer {

    private static final ResourceLocation BLIGHTED_SKELETON_TEXTURE =
            ArsZero.prefix("textures/entity/skeleton/blighted_skeleton.png");

    public MageSkeletonRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new MageSkeletonGlintLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSkeleton entity) {
        return BLIGHTED_SKELETON_TEXTURE;
    }
}
