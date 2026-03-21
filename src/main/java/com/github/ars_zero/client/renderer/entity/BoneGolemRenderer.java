package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.ArsZeroClient;
import com.github.ars_zero.client.renderer.entity.model.BoneGolemModel;
import com.github.ars_zero.common.entity.BoneGolem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BoneGolemRenderer extends MobRenderer<BoneGolem, BoneGolemModel<BoneGolem>> {

    private static final ResourceLocation TEXTURE = ArsZero.prefix("textures/entity/bone_golem.png");

    public BoneGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new BoneGolemModel<>(context.bakeLayer(ArsZeroClient.BONE_GOLEM_LAYER)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(BoneGolem entity) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(BoneGolem entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        if (!((double) entity.walkAnimation.speed() < 0.01)) {
            float f1 = entity.walkAnimation.position(partialTick) + 6.0F;
            float f2 = (Math.abs(f1 % 13.0F - 6.5F) - 3.25F) / 3.25F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.5F * f2));
        }
    }
}
