package com.github.ars_zero.client.renderer;

import com.github.ars_zero.ArsZero;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

@EventBusSubscriber(modid = ArsZero.MOD_ID, value = Dist.CLIENT)
public class ArsZeroShaders {
    
    public static ShaderInstance ANIMATED_VOXEL;
    
    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        ResourceProvider resourceProvider = event.getResourceProvider();
        
        event.registerShader(
            new ShaderInstance(
                resourceProvider,
                ResourceLocation.fromNamespaceAndPath("minecraft", "animated_voxel"),
                DefaultVertexFormat.NEW_ENTITY
            ),
            shader -> ANIMATED_VOXEL = shader
        );
    }
}

