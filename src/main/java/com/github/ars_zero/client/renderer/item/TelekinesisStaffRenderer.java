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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.Color;

import javax.annotation.Nullable;

/**
 * Renders the Staff of Telekinesis with custom model, texture, and animations (telekinesis_staff.*).
 */
public class TelekinesisStaffRenderer extends GeoItemRenderer<AbstractStaff> {

    private static final ResourceLocation TEXTURE = ArsZero.prefix("textures/item/telekinesis_staff.png");

    public TelekinesisStaffRenderer() {
        super(new TelekinesisStaffModel());
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractStaff animatable) {
        return TEXTURE;
    }

    @Override
    public RenderType getRenderType(AbstractStaff animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
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

        MultiBufferSource.BufferSource var10000;
        if (bufferSource instanceof MultiBufferSource.BufferSource bufferSource2) {
            var10000 = bufferSource2;
        } else {
            var10000 = Minecraft.getInstance().renderBuffers().bufferSource();
        }
        MultiBufferSource.BufferSource defaultBufferSource = var10000;
        RenderType renderType = this.getRenderType(this.animatable, this.getTextureLocation(this.animatable), defaultBufferSource, partialTick);
        VertexConsumer buffer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, true, this.currentItemStack != null && this.currentItemStack.hasFoil());

        poseStack.pushPose();

        poseStack.translate(-0.1F, 0.25f, 0F);
        poseStack.scale(0.47F, 0.47F, 0.47F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45));
        poseStack.mulPose(Axis.XP.rotationDegrees(10));

        this.defaultRenderGui(poseStack, this.animatable, defaultBufferSource, renderType, buffer, 0.0F, 0.0F, packedLight, packedOverlay, color);
        defaultBufferSource.endBatch();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        poseStack.popPose();
    }

    /** Same pipeline as SpellStaffRenderer for consistent GUI/hand rendering. */
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
}
