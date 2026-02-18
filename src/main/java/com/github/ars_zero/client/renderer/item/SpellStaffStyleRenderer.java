package com.github.ars_zero.client.renderer.item;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractStaff;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.Color;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Renders AbstractStaff items (e.g. StaticStaff) with the spell staff model, tier bone visibility, and dye-based texture.
 */
public class SpellStaffStyleRenderer extends GeoItemRenderer<AbstractStaff> {

    private final Set<String> hiddenBones;

    public SpellStaffStyleRenderer(Set<String> hiddenBones) {
        super(new SpellStaffModelForStaff());
        this.hiddenBones = hiddenBones;
    }

    @Override
    public void preRender(PoseStack poseStack, AbstractStaff animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        resetBoneVisibility(model, "tier2");
        resetBoneVisibility(model, "tier3");

        for (String boneName : hiddenBones) {
            model.getBone(boneName).ifPresent(bone -> {
                bone.setHidden(true);
                hideChildBones(bone);
            });
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    private void resetBoneVisibility(BakedGeoModel model, String boneName) {
        model.getBone(boneName).ifPresent(bone -> {
            bone.setHidden(false);
            resetChildBoneVisibility(bone);
        });
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
    protected void renderInGui(ItemDisplayContext transformType, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, float partialTick) {
        this.animatable.getAnimatableInstanceCache().getManagerForId(this.animatable.hashCode()).getAnimationControllers().values().forEach(controller -> controller.stop());

        if (this.useEntityGuiLighting) {
            Lighting.setupForEntityInInventory();
        } else {
            Lighting.setupForFlatItems();
        }
        int color = Color.ofOpaque(0xFFFFFF).argbInt();

        MultiBufferSource.BufferSource defaultBufferSource = bufferSource instanceof MultiBufferSource.BufferSource bs ? bs : Minecraft.getInstance().renderBuffers().bufferSource();
        RenderType renderType = this.getRenderType(this.animatable, this.getTextureLocation(this.animatable), defaultBufferSource, partialTick);
        VertexConsumer buffer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, true, this.currentItemStack != null && this.currentItemStack.hasFoil());

        poseStack.pushPose();
        poseStack.translate(0F, 0.35, 0F);
        poseStack.scale(0.47F, 0.47F, 0.47F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45));
        poseStack.mulPose(Axis.XP.rotationDegrees(10));

        this.defaultRenderGui(poseStack, this.animatable, defaultBufferSource, renderType, buffer, 0.0F, 0.0F, packedLight, packedOverlay, color);
        defaultBufferSource.endBatch();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        poseStack.popPose();
    }

    public void defaultRenderGui(PoseStack poseStack, AbstractStaff animatable, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight, int packedOverlay, int packedColor) {
        poseStack.pushPose();
        BakedGeoModel model = this.model.getBakedModel(this.model.getModelResource(animatable));
        if (renderType == null) {
            renderType = this.getRenderType(animatable, this.getTextureLocation(animatable), bufferSource, partialTick);
        }
        if (buffer == null) {
            buffer = bufferSource.getBuffer(renderType);
        }

        this.preRender(poseStack, animatable, model, bufferSource, buffer, false, partialTick, packedLight, packedOverlay, packedColor);
        if (this.firePreRenderEvent(poseStack, model, bufferSource, partialTick, packedLight)) {
            this.preApplyRenderLayers(poseStack, animatable, model, renderType, bufferSource, buffer, (float) packedLight, packedLight, packedOverlay);
            this.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, false, partialTick, packedLight, packedOverlay, packedColor);
            this.applyRenderLayers(poseStack, animatable, model, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            this.postRender(poseStack, animatable, model, bufferSource, buffer, false, partialTick, packedLight, packedOverlay, packedColor);
            this.firePostRenderEvent(poseStack, model, bufferSource, partialTick, packedLight);
        }

        poseStack.popPose();
        this.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, packedColor);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractStaff o) {
        String base = "textures/item/spell_staff_";
        var dyeColor = currentItemStack != null ? currentItemStack.get(DataComponents.BASE_COLOR) : null;
        String color = dyeColor == null ? "purple" : dyeColor.getName();
        return ArsZero.prefix(base + color + ".png");
    }

    @Override
    public RenderType getRenderType(AbstractStaff animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
