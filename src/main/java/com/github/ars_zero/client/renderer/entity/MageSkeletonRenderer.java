package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.renderer.entity.model.BlightedSkeletonModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Skeleton;

/**
 * Skeleton renderer for the Mage Skeleton, adding an enchanted glint layer on the skeleton body only.
 * Uses the custom blighted_skeleton.png texture (vanilla skeleton base) and BlightedSkeletonModel for both-arms-up cast pose.
 */
public class MageSkeletonRenderer extends HumanoidMobRenderer<Skeleton, BlightedSkeletonModel> {

    public static final ModelLayerLocation BLIGHTED_SKELETON_LAYER =
            new ModelLayerLocation(ArsZero.prefix("blighted_skeleton"), "main");

    private static final ResourceLocation BLIGHTED_SKELETON_TEXTURE =
            ArsZero.prefix("textures/entity/skeleton/blighted_skeleton.png");

    public MageSkeletonRenderer(EntityRendererProvider.Context context) {
        super(context, new BlightedSkeletonModel(context.bakeLayer(BLIGHTED_SKELETON_LAYER)), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this,
                new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)),
                new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR)),
                context.getModelManager()));
        addLayer(new MageSkeletonGlintLayer(this));
        addLayer(new LichEyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Skeleton entity) {
        return BLIGHTED_SKELETON_TEXTURE;
    }
}
