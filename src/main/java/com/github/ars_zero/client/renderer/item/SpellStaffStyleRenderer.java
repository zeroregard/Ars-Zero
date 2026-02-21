package com.github.ars_zero.client.renderer.item;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractStaff;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Renders AbstractStaff items with the spell staff model, optional tier bone visibility, and dye-based texture.
 */
public class SpellStaffStyleRenderer extends GeoItemRenderer<AbstractStaff> {

    private final Set<String> hiddenBones;

    public SpellStaffStyleRenderer(Set<String> hiddenBones) {
        super(new SpellStaffModelForStaff());
        this.hiddenBones = hiddenBones;
    }

    @Override
    public void preRender(com.mojang.blaze3d.vertex.PoseStack poseStack, AbstractStaff animatable, BakedGeoModel model,
                         net.minecraft.client.renderer.MultiBufferSource bufferSource,
                         com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick,
                         int packedLight, int packedOverlay, int color) {
        if (model.getBone("tier2").isPresent()) {
            model.getBone("tier2").ifPresent(bone -> {
                bone.setHidden(false);
                resetChildBoneVisibility(bone);
            });
        }
        if (model.getBone("tier3").isPresent()) {
            model.getBone("tier3").ifPresent(bone -> {
                bone.setHidden(false);
                resetChildBoneVisibility(bone);
            });
        }
        for (String boneName : hiddenBones) {
            model.getBone(boneName).ifPresent(bone -> {
                bone.setHidden(true);
                hideChildBones(bone);
            });
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    private void resetChildBoneVisibility(GeoBone bone) {
        for (GeoBone child : bone.getChildBones()) {
            child.setHidden(false);
            resetChildBoneVisibility(child);
        }
    }

    private void hideChildBones(GeoBone bone) {
        for (GeoBone child : bone.getChildBones()) {
            child.setHidden(true);
            hideChildBones(child);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractStaff o) {
        String base = "textures/item/spell_staff_";
        var dyeColor = currentItemStack != null ? currentItemStack.get(DataComponents.BASE_COLOR) : null;
        String color = dyeColor == null ? "purple" : dyeColor.getSerializedName();
        return ArsZero.prefix(base + color + ".png");
    }

    @Override
    public RenderType getRenderType(AbstractStaff animatable, ResourceLocation texture, @Nullable net.minecraft.client.renderer.MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
