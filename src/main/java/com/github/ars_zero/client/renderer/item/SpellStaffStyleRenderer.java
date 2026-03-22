package com.github.ars_zero.client.renderer.item;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractStaff;
import com.github.ars_zero.common.item.FilialItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.Set;

/**
 * Renders AbstractStaff items with the spell staff model, optional tier bone visibility, and dye-based texture.
 */
public class SpellStaffStyleRenderer extends AbstractStaffRenderer<AbstractStaff> {

    private final Set<String> hiddenBones;

    public SpellStaffStyleRenderer(Set<String> hiddenBones) {
        super(new SpellStaffModelForStaff());
        this.hiddenBones = hiddenBones;
    }

    @Override
    protected void applyGuiTransform(PoseStack poseStack) {
            poseStack.translate(-0.05F, 0.25, 0F);
    }

    @Override
    public void preRender(PoseStack poseStack, AbstractStaff animatable, BakedGeoModel model,
                         MultiBufferSource bufferSource,
                         VertexConsumer buffer, boolean isReRender, float partialTick,
                         int packedLight, int packedOverlay, int color) {
        model.getBone("tier2").ifPresent(bone -> { bone.setHidden(false); resetChildBoneVisibility(bone); });
        model.getBone("tier3").ifPresent(bone -> { bone.setHidden(false); resetChildBoneVisibility(bone); });

        String school = currentItemStack != null ? FilialItem.getStaffFilialSchool(currentItemStack) : null;
        boolean hasFilial = school != null;

        setBoneHidden(model, "filial",          !hasFilial);
        setBoneHidden(model, "no_filial",        hasFilial);
        setBoneHidden(model, "source_gem",       hasFilial);
        setBoneHidden(model, "tier2_filial",    !hasFilial);
        setBoneHidden(model, "tier2_no_filial",  hasFilial);
        setBoneHidden(model, "tier3_filial",    !hasFilial);
        setBoneHidden(model, "tier3_no_filial",  hasFilial);

        for (String boneName : hiddenBones) {
            model.getBone(boneName).ifPresent(bone -> {
                bone.setHidden(true);
                hideChildBones(bone);
            });
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    @Override
    public void postRender(PoseStack poseStack, AbstractStaff animatable, BakedGeoModel model,
            MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);

        if (isReRender || currentItemStack == null) return;

        String school = FilialItem.getStaffFilialSchool(currentItemStack);
        if (school == null) return;

        FilialItem filialItem = FilialItem.getItemForSchool(school);
        if (filialItem == null) return;

        model.getBone("filial").ifPresent(bone -> {
            ItemStack filialStack = new ItemStack(filialItem);
            poseStack.pushPose();
            poseStack.translate(bone.getPivotX() / 16f, bone.getPivotY() / 16f, bone.getPivotZ() / 16f);
            // Bob: 2px amplitude, 2-second (40-tick) period
            float time = (Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0) + partialTick;
            float bob = (float) Math.sin(time * Math.PI / 20.0) * (0.5f / 16f);
            poseStack.translate(0, bob, 0);
            // Spin: one full rotation every 8 seconds (160 ticks)
            if (filialItem.shouldSpinOnStaff()) {
                poseStack.mulPose(Axis.YP.rotationDegrees(time * (360f / 160f)));
            }
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    filialStack, ItemDisplayContext.FIXED,
                    packedLight, packedOverlay,
                    poseStack, bufferSource,
                    Minecraft.getInstance().level, 0);
            poseStack.popPose();
        });
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractStaff o) {
        String base = "textures/item/spell_staff_";
        var dyeColor = currentItemStack != null ? currentItemStack.get(DataComponents.BASE_COLOR) : null;
        String color = dyeColor == null ? "purple" : dyeColor.getSerializedName();
        return ArsZero.prefix(base + color + ".png");
    }

    @Override
    public RenderType getRenderType(AbstractStaff animatable, ResourceLocation texture, @org.jetbrains.annotations.Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
