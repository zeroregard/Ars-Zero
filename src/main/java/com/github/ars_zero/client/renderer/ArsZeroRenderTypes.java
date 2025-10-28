package com.github.ars_zero.client.renderer;

import com.github.ars_zero.ArsZero;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class ArsZeroRenderTypes extends RenderType {
    
    public ArsZeroRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
    
    public static RenderType animatedVoxel(ResourceLocation texture, boolean emissive) {
        System.out.println("=== ArsZeroRenderTypes.animatedVoxel ===");
        System.out.println("Creating render type - Emissive: " + emissive);
        System.out.println("Shader: " + ArsZeroShaders.ANIMATED_VOXEL);
        
        CompositeState.CompositeStateBuilder builder = CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(() -> ArsZeroShaders.ANIMATED_VOXEL))
            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOverlayState(OVERLAY)
            .setCullState(NO_CULL);
        
        if (emissive) {
            System.out.println("Using COLOR_WRITE (no lightmap)");
            builder.setWriteMaskState(COLOR_WRITE);
        } else {
            System.out.println("Using LIGHTMAP + COLOR_DEPTH_WRITE");
            builder.setLightmapState(LIGHTMAP);
            builder.setWriteMaskState(COLOR_DEPTH_WRITE);
        }
        
        RenderType result = create(
            emissive ? "animated_voxel_emissive" : "animated_voxel",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            true,
            true,
            builder.createCompositeState(emissive)
        );
        
        System.out.println("Created RenderType: " + result);
        return result;
    }
}

