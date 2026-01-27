package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.client.renderer.ArsZeroRenderTypes;
import com.github.ars_zero.client.renderer.entity.model.ArcaneCircleEntityModel;
import com.github.ars_zero.common.entity.ArcaneCircleEntity;
import com.github.ars_zero.common.casting.CastingStyle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

import java.util.HashSet;
import java.util.Set;

public class ArcaneCircleEntityRenderer extends GeoEntityRenderer<ArcaneCircleEntity> {

    private static final Set<String> BORDER_BONES = Set.of(
        "alphabet", "circle_big", "circle_small", "circle_big_thick", "triangle_big"
    );
    private static final Set<String> SYMBOL_BONES = Set.of(
        "pentagram_big", "pentagram_small", "triangle_small",
        "school_fire", "school_water", "school_earth", "school_air",
        "school_abjuration", "school_anima", "school_conjuration", "school_manipulation"
    );
    private static final Set<String> ALL_BONES = new HashSet<>();
    static {
        ALL_BONES.addAll(BORDER_BONES);
        ALL_BONES.add("root");
        ALL_BONES.addAll(SYMBOL_BONES);
    }

    public ArcaneCircleEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new ArcaneCircleEntityModel());
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }

    @Override
    public RenderType getRenderType(ArcaneCircleEntity animatable, ResourceLocation texture,
            MultiBufferSource bufferSource, float partialTick) {
        return ArsZeroRenderTypes.entityTranslucentEmissiveFullBright(texture);
    }

    @Override
    public void preRender(PoseStack poseStack, ArcaneCircleEntity animatable, BakedGeoModel model,
            MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
            int packedLight, int packedOverlay, int color) {
        
        model.getBone("root").ifPresent(bone -> {
            bone.setHidden(false);
        });
        
        for (String boneName : ALL_BONES) {
            if ("root".equals(boneName)) {
                continue;
            }
            model.getBone(boneName).ifPresent(bone -> {
                bone.setHidden(true);
                hideChildBones(bone);
            });
        }
        
        CastingStyle style = animatable.getStyle();
        if (style != null && style.isEnabled()) {
            Set<String> toShow = bonesToShow(style, animatable);
            for (String boneName : toShow) {
                if ("root".equals(boneName)) {
                    continue;
                }
                model.getBone(boneName).ifPresent(bone -> {
                    bone.setHidden(false);
                    resetChildBoneVisibility(bone);
                });
            }
        }

        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, color);
    }

    private void hideChildBones(GeoBone bone) {
        for (GeoBone child : bone.getChildBones()) {
            child.setHidden(true);
            hideChildBones(child);
        }
    }

    private void resetChildBoneVisibility(GeoBone bone) {
        for (GeoBone child : bone.getChildBones()) {
            child.setHidden(false);
            resetChildBoneVisibility(child);
        }
    }

    private Set<String> bonesToShow(CastingStyle style, ArcaneCircleEntity animatable) {
        Set<String> out = new HashSet<>();
        for (String name : style.getActiveBones()) {
            if (BORDER_BONES.contains(name)) {
                out.add(name);
            }
        }
        if (style.isSymbolAuto()) {
            String schoolId = animatable.getCurrentSchoolId();
            if (schoolId != null && SYMBOL_BONES.contains(schoolId)) {
                out.add(schoolId);
            }
        } else {
            String sym = style.getSelectedSymbolBone();
            if (sym != null && SYMBOL_BONES.contains(sym)) {
                out.add(sym);
            }
        }
        return out;
    }

    @Override
    public Color getRenderColor(ArcaneCircleEntity animatable, float partialTick, int packedLight) {
        int colorInt = animatable.getColor();
        int r = (colorInt >> 16) & 0xFF;
        int g = (colorInt >> 8) & 0xFF;
        int b = colorInt & 0xFF;
        return Color.ofRGBA(r / 255.0f, g / 255.0f, b / 255.0f, 1.0f);
    }

    @Override
    public void render(ArcaneCircleEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, LightTexture.FULL_BRIGHT);
    }

    @Override
    protected void applyRotations(ArcaneCircleEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw,
            float partialTick, float nativeScale) {
        CastingStyle style = animatable.getStyle();
        if (style != null && style.getPlacement() == CastingStyle.Placement.NEAR) {
            float yaw = animatable.getSyncedYRot();
            float pitch = animatable.getSyncedXRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(90f - pitch));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - rotationYaw));
        }
    }

    @Override
    public void actuallyRender(PoseStack poseStack, ArcaneCircleEntity animatable, BakedGeoModel model,
            RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, int color) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
    }
}
