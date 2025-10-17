package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class WaterVoxelEntityRenderer extends BaseVoxelEntityRenderer<WaterVoxelEntity> {
    
    public WaterVoxelEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public ResourceLocation getTextureLocation(WaterVoxelEntity animatable) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
    
    @Override
    public void actuallyRender(PoseStack poseStack, WaterVoxelEntity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer()
            .getBlockModel(Blocks.WATER.defaultBlockState())
            .getParticleIcon();
        
        int entityColor = animatable.getColor();
        int alpha = (int)(0.3f * 255);
        int finalColor = entityColor | (alpha << 24);
        
        RenderType waterRenderType = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
        
        super.actuallyRender(poseStack, animatable, model, waterRenderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, finalColor);
    }
}
