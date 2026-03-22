package com.github.ars_zero.client.renderer.item;

import com.github.ars_zero.client.renderer.model.FilialGeoModel;
import com.github.ars_zero.common.item.FilialItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class CreativeFilialItemRenderer extends FilialItemRenderer {

    public CreativeFilialItemRenderer(FilialGeoModel model) {
        super(model);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, FilialItem animatable, BakedGeoModel model,
            RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
            boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        float hue = ((Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime()
                : 0L) * 2f + partialTick * 2f) % 360f;
        int rgb = hsvToRgb(hue, 1f, 1f);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, 0xFF000000 | rgb);
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        int h = (int) (hue / 60) % 6;
        float f = hue / 60f - (int) (hue / 60f);
        int p = Math.round(value * (1f - saturation) * 255f);
        int q = Math.round(value * (1f - f * saturation) * 255f);
        int t = Math.round(value * (1f - (1f - f) * saturation) * 255f);
        int v = Math.round(value * 255f);
        return switch (h) {
            case 0 -> (v << 16) | (t << 8) | p;
            case 1 -> (q << 16) | (v << 8) | p;
            case 2 -> (p << 16) | (v << 8) | t;
            case 3 -> (p << 16) | (q << 8) | v;
            case 4 -> (t << 16) | (p << 8) | v;
            default -> (v << 16) | (p << 8) | q;
        };
    }
}
