package com.github.ars_zero.client.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SkeletonRenderer;

/**
 * Skeleton renderer for the Mage Skeleton, adding an enchanted glint layer on the skeleton body only.
 */
public class MageSkeletonRenderer extends SkeletonRenderer {

    public MageSkeletonRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new MageSkeletonGlintLayer(this));
    }
}
