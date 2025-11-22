package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.GrappleTetherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class GrappleTetherEntityRenderer extends EntityRenderer<GrappleTetherEntity> {
    
    public GrappleTetherEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public void render(GrappleTetherEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    }
    
    @Override
    public net.minecraft.resources.ResourceLocation getTextureLocation(GrappleTetherEntity entity) {
        return null;
    }
}
