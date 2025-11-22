package com.github.ars_zero.client.renderer;

import com.github.ars_zero.common.entity.GrappleTetherEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = com.github.ars_zero.ArsZero.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class TetherLineRenderer {
    
    private static int renderCallCount = 0;
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        
        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        
        poseStack.pushPose();
        
        Vec3 cameraPos = camera.getPosition();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        
        int tetherCount = 0;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof GrappleTetherEntity tether && tether.isAlive()) {
                tetherCount++;
                renderTetherLine(tether, poseStack, bufferSource, mc, partialTick, cameraPos);
            }
        }
        
        if (tetherCount > 0 && renderCallCount++ % 20 == 0) {
            com.github.ars_zero.ArsZero.LOGGER.info("[Tether Renderer] Rendering {} tether(s)", tetherCount);
        }
        
        poseStack.popPose();
        bufferSource.endBatch();
    }
    
    private static void renderTetherLine(GrappleTetherEntity tether, PoseStack poseStack, MultiBufferSource bufferSource, Minecraft mc, float partialTick, Vec3 cameraPos) {
        // On client, we can't reliably get playerUUID, so just use the local player
        // Tethers are almost always for the local player anyway
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        
        Vec3 playerPos = player.getPosition(partialTick).add(0, player.getEyeHeight() * 0.5, 0);
        Vec3 targetPos = getTargetPosition(tether, mc.level, partialTick);
        
        if (targetPos == null) {
            return;
        }
        
        float distance = (float) playerPos.distanceTo(targetPos);
        float maxLength = tether.getMaxLength();
        
        float r, g, b;
        if (distance > maxLength) {
            r = 1.0f;
            g = 0.1f;
            b = 0.1f;
        } else {
            float ratio = distance / maxLength;
            r = 0.1f + (ratio * 0.4f);
            g = 0.8f - (ratio * 0.3f);
            b = 1.0f;
        }
        
        // Render thick visible line with multiple passes for visibility
        renderThickLine(poseStack, bufferSource, playerPos, targetPos, r, g, b, cameraPos);
        
        // Also render particles along the line for better visibility
        renderParticleLine(mc.level, playerPos, targetPos, r, g, b, distance, maxLength, partialTick);
        
        // Render debug boxes at endpoints
        renderDebugBox(poseStack, bufferSource, playerPos, 1.0f, 0.0f, 1.0f); // Magenta at player
        renderDebugBox(poseStack, bufferSource, targetPos, 0.0f, 1.0f, 1.0f); // Cyan at target
        
        // Render distance text indicator
        if (distance > maxLength * 0.9f) {
            Vec3 midPoint = playerPos.add(targetPos).scale(0.5);
            renderDebugBox(poseStack, bufferSource, midPoint, r, g, b); // Warning box in middle
        }
    }
    
    private static Vec3 getTargetPosition(GrappleTetherEntity tether, net.minecraft.client.multiplayer.ClientLevel level, float partialTick) {
        if (tether.isBlockTarget()) {
            BlockPos targetPos = tether.getTargetPos();
            if (targetPos != null) {
                return Vec3.atCenterOf(targetPos);
            }
        } else {
            var targetEntityUUID = tether.getTargetEntityUUID();
            if (targetEntityUUID != null && level != null) {
                for (var entity : level.entitiesForRendering()) {
                    if (entity.getUUID().equals(targetEntityUUID) && entity.isAlive()) {
                        return entity.getPosition(partialTick);
                    }
                }
            }
        }
        return null;
    }
    
    private static void renderThickLine(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 start, Vec3 end, float r, float g, float b, Vec3 cameraPos) {
        // Use debug rendering with multiple line strips for thickness
        Matrix4f matrix = poseStack.last().pose();
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 dir = direction.normalize();
        
        // Create a thick line by rendering multiple parallel segments
        int segments = Math.max(10, (int)(distance * 8));
        int color = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255) | 0xFF000000;
        
        // Render main thick line with multiple passes
        for (int pass = 0; pass < 5; pass++) {
            VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.debugLineStrip(2.0 + pass * 0.5));
            
            float prevX = (float)start.x;
            float prevY = (float)start.y;
            float prevZ = (float)start.z;
            
            for (int i = 1; i <= segments; i++) {
                double t = (double)i / segments;
                Vec3 point = start.add(dir.scale(distance * t));
                float x = (float)point.x;
                float y = (float)point.y;
                float z = (float)point.z;
                
                vertexConsumer.addVertex(matrix, prevX, prevY, prevZ)
                        .setColor(color)
                        .setNormal((float)dir.x, (float)dir.y, (float)dir.z);
                
                vertexConsumer.addVertex(matrix, x, y, z)
                        .setColor(color)
                        .setNormal((float)dir.x, (float)dir.y, (float)dir.z);
                
                prevX = x;
                prevY = y;
                prevZ = z;
            }
        }
        
        // Also render a simpler direct line for maximum visibility with extra thickness
        VertexConsumer mainLine = bufferSource.getBuffer(RenderType.debugLineStrip(6.0));
        int mainColor = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255) | 0xFF000000;
        
        mainLine.addVertex(matrix, (float)start.x, (float)start.y, (float)start.z)
                .setColor(mainColor)
                .setNormal((float)dir.x, (float)dir.y, (float)dir.z);
        
        mainLine.addVertex(matrix, (float)end.x, (float)end.y, (float)end.z)
                .setColor(mainColor)
                .setNormal((float)dir.x, (float)dir.y, (float)dir.z);
    }
    
    private static void renderDebugBox(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 pos, float r, float g, float b) {
        renderDebugBox(poseStack, bufferSource, pos, r, g, b, 0.2f);
    }
    
    private static void renderDebugBox(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 pos, float r, float g, float b, float size) {
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.debugLineStrip(3.0));
        
        int color = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255) | 0xFF000000;
        float halfSize = size / 2.0f;
        
        float x = (float)pos.x;
        float y = (float)pos.y;
        float z = (float)pos.z;
        
        // Render a small cube outline
        // Bottom square
        renderBoxLine(vertexConsumer, matrix, x - halfSize, y - halfSize, z - halfSize, x + halfSize, y - halfSize, z - halfSize, color);
        renderBoxLine(vertexConsumer, matrix, x + halfSize, y - halfSize, z - halfSize, x + halfSize, y - halfSize, z + halfSize, color);
        renderBoxLine(vertexConsumer, matrix, x + halfSize, y - halfSize, z + halfSize, x - halfSize, y - halfSize, z + halfSize, color);
        renderBoxLine(vertexConsumer, matrix, x - halfSize, y - halfSize, z + halfSize, x - halfSize, y - halfSize, z - halfSize, color);
        
        // Top square
        renderBoxLine(vertexConsumer, matrix, x - halfSize, y + halfSize, z - halfSize, x + halfSize, y + halfSize, z - halfSize, color);
        renderBoxLine(vertexConsumer, matrix, x + halfSize, y + halfSize, z - halfSize, x + halfSize, y + halfSize, z + halfSize, color);
        renderBoxLine(vertexConsumer, matrix, x + halfSize, y + halfSize, z + halfSize, x - halfSize, y + halfSize, z + halfSize, color);
        renderBoxLine(vertexConsumer, matrix, x - halfSize, y + halfSize, z + halfSize, x - halfSize, y + halfSize, z - halfSize, color);
        
        // Vertical edges
        renderBoxLine(vertexConsumer, matrix, x - halfSize, y - halfSize, z - halfSize, x - halfSize, y + halfSize, z - halfSize, color);
        renderBoxLine(vertexConsumer, matrix, x + halfSize, y - halfSize, z - halfSize, x + halfSize, y + halfSize, z - halfSize, color);
        renderBoxLine(vertexConsumer, matrix, x + halfSize, y - halfSize, z + halfSize, x + halfSize, y + halfSize, z + halfSize, color);
        renderBoxLine(vertexConsumer, matrix, x - halfSize, y - halfSize, z + halfSize, x - halfSize, y + halfSize, z + halfSize, color);
    }
    
    private static void renderBoxLine(VertexConsumer vertexConsumer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        vertexConsumer.addVertex(matrix, x1, y1, z1)
                .setColor(color);
        vertexConsumer.addVertex(matrix, x2, y2, z2)
                .setColor(color);
    }
    
    private static void renderParticleLine(Level level, Vec3 start, Vec3 end, float r, float g, float b, float distance, float maxLength, float partialTick) {
        if (level == null || level.isClientSide == false) {
            return;
        }
        
        Vec3 direction = end.subtract(start);
        double dirLength = direction.length();
        Vec3 dir = direction.normalize();
        
        // Render particles along the line for visibility
        int particleCount = Math.max(5, Math.min(50, (int)(distance * 2)));
        
        for (int i = 0; i < particleCount; i++) {
            double t = (double)i / particleCount;
            Vec3 pos = start.add(dir.scale(dirLength * t));
            
            // Only render particles every few ticks to avoid lag
            if ((int)((pos.x + pos.y + pos.z) * 100) % 3 == 0) {
                Vector3f colorVec = new Vector3f(r, g, b);
                DustParticleOptions particle = new DustParticleOptions(colorVec, 2.0f);
                
                // Spawn particle at this position
                level.addParticle(
                    particle,
                    pos.x, pos.y, pos.z,
                    0, 0, 0
                );
            }
        }
    }
}

