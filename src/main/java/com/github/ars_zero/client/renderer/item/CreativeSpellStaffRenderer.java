package com.github.ars_zero.client.renderer.item;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;

public class CreativeSpellStaffRenderer extends AbstractStaffRenderer<AbstractSpellStaff> {

    public CreativeSpellStaffRenderer() {
        super(new SpellStaffModel());
    }

    @Override
    protected void applyGuiTransform(PoseStack poseStack) {
        poseStack.translate(-0.05F, 0.25, 0F);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSpellStaff o) {
        String base = "textures/item/spell_staff_";
        var dyeColor = currentItemStack.get(DataComponents.BASE_COLOR);
        String color = dyeColor == null ? "purple" : dyeColor.getName();
        return ArsZero.prefix(base + color + ".png");
    }

    @Override
    public RenderType getRenderType(AbstractSpellStaff animatable, ResourceLocation texture, @org.jetbrains.annotations.Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
