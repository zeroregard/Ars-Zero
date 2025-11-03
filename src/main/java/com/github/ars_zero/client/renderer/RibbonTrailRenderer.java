package com.github.ars_zero.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

public class RibbonTrailRenderer {
    
    public static void renderTrail(List<Vec3> trailPoints, PoseStack poseStack, MultiBufferSource bufferSource, 
                                   int packedLight, float width, int color, float alpha) {
        if (trailPoints.size() < 2) {
            return;
        }
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/misc/white.png")
        ));
        
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        for (int i = 0; i < trailPoints.size() - 1; i++) {
            Vec3 p1 = trailPoints.get(i);
            Vec3 p2 = trailPoints.get(i + 1);
            
            float fade = (float) i / (trailPoints.size() - 1);
            float widthAtPoint = width * (1.0f - fade * 0.7f);
            float alphaAtPoint = alpha * (1.0f - fade);
            
            Vec3 toCamera = cameraPos.subtract(p1).normalize();
            Vec3 direction = p2.subtract(p1).normalize();
            Vec3 perpendicular = direction.cross(toCamera).normalize();
            
            Vec3 offset = perpendicular.scale(widthAtPoint / 2.0);
            
            Vec3 v1 = p1.add(offset);
            Vec3 v2 = p1.subtract(offset);
            Vec3 v3 = p2.subtract(offset);
            Vec3 v4 = p2.add(offset);
            
            Vector3f normalVec = new Vector3f((float)toCamera.x, (float)toCamera.y, (float)toCamera.z);
            normal.transform(normalVec);
            
            addVertex(vertexConsumer, pose, (float)v1.x, (float)v1.y, (float)v1.z, 0, 0, normalVec, r, g, b, alphaAtPoint, packedLight);
            addVertex(vertexConsumer, pose, (float)v2.x, (float)v2.y, (float)v2.z, 1, 0, normalVec, r, g, b, alphaAtPoint, packedLight);
            addVertex(vertexConsumer, pose, (float)v3.x, (float)v3.y, (float)v3.z, 1, 1, normalVec, r, g, b, alphaAtPoint, packedLight);
            addVertex(vertexConsumer, pose, (float)v4.x, (float)v4.y, (float)v4.z, 0, 1, normalVec, r, g, b, alphaAtPoint, packedLight);
        }
    }
    
    private static void addVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, 
                                  float u, float v, Vector3f normal, float r, float g, float b, float a, int light) {
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(normal.x, normal.y, normal.z);
    }
}


