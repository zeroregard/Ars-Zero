package com.github.ars_zero.client.renderer.entity;

import com.github.ars_zero.common.entity.AbstractGeometryProcessEntity;
import com.github.ars_zero.common.entity.IGeometryProcessEntity.BlockStatus;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;

/**
 * Shared renderer for geometry-processing entities (Terrain, Break, etc.)
 */
public class AbstractGeometryEntityRenderer<T extends AbstractGeometryProcessEntity & GeoEntity>
        extends GeoEntityRenderer<T> {

    protected static final float CLEAR_RED = 0.2f;
    protected static final float CLEAR_GREEN = 0.9f;
    protected static final float CLEAR_BLUE = 0.35f;

    protected static final float ADJACENT_RED = 0.9f;
    protected static final float ADJACENT_GREEN = 0.9f;
    protected static final float ADJACENT_BLUE = 0.2f;

    protected static final float BLOCKED_RED = 0.9f;
    protected static final float BLOCKED_GREEN = 0.2f;
    protected static final float BLOCKED_BLUE = 0.2f;

    protected static final double RENDER_DISTANCE = 128.0;
    protected static final float LINE_ALPHA = 0.9f;
    
    protected static final RenderType LINES_SEE_THROUGH = RenderType.create(
        "geometry_lines_see_through",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINES,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.0)))
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setCullState(RenderStateShard.NO_CULL)
            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
            .createCompositeState(false)
    );

    public AbstractGeometryEntityRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture,
            MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {

        if (entity.isBuilding()) {
            poseStack.pushPose();
            poseStack.translate(0, 0.5, 0);

            float yaw = calculateYawToCenter(entity);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            poseStack.popPose();
            return;
        }

        if (entity.getLifespan() <= 0) {
            return;
        }

        Player clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer != null) {
            UUID casterUuid = entity.getCasterUUID();
            if (casterUuid != null && casterUuid.equals(clientPlayer.getUUID())) {
                renderPreviewOutline(entity, poseStack, buffer, partialTicks);
            }
        }
    }

    protected float calculateYawToCenter(T entity) {
        BlockPos center = entity.getEffectiveCenter();
        Vec3 entityPos = entity.position();
        double dx = (center.getX() + 0.5) - entityPos.x;
        double dz = (center.getZ() + 0.5) - entityPos.z;

        return (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
    }

    protected void renderPreviewOutline(T entity, PoseStack poseStack,
            MultiBufferSource buffer, float partialTicks) {
        BlockPos effectiveCenter = entity.getEffectiveCenter();
        Vec3 effectiveCenterVec = Vec3.atCenterOf(effectiveCenter);
        Vec3 entityRenderPos = entity.getPosition(partialTicks);
        Vec3 offsetToEffective = effectiveCenterVec.subtract(entityRenderPos);
        
        Map<BlockPos, BlockStatus> blockStatuses = entity.getBlockStatuses();
        
        if (blockStatuses.isEmpty()) {
            return;
        }

        Set<BlockPos> allPositions = new HashSet<>(blockStatuses.keySet());
        
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        
        for (BlockPos blockPos : allPositions) {
            minX = Math.min(minX, blockPos.getX());
            minY = Math.min(minY, blockPos.getY());
            minZ = Math.min(minZ, blockPos.getZ());
            maxX = Math.max(maxX, blockPos.getX());
            maxY = Math.max(maxY, blockPos.getY());
            maxZ = Math.max(maxZ, blockPos.getZ());
        }

        poseStack.pushPose();
        poseStack.translate(offsetToEffective.x, offsetToEffective.y, offsetToEffective.z);

        for (Map.Entry<BlockPos, BlockStatus> entry : blockStatuses.entrySet()) {
            BlockPos blockPos = entry.getKey();
            
            if (!isOnShell(blockPos, allPositions)) {
                continue;
            }
            
            BlockStatus status = entry.getValue();

            float r, g, b;
            switch (status) {
                case BLOCKED -> {
                    r = BLOCKED_RED;
                    g = BLOCKED_GREEN;
                    b = BLOCKED_BLUE;
                }
                case ADJACENT -> {
                    r = ADJACENT_RED;
                    g = ADJACENT_GREEN;
                    b = ADJACENT_BLUE;
                }
                default -> {
                    r = CLEAR_RED;
                    g = CLEAR_GREEN;
                    b = CLEAR_BLUE;
                }
            }

            double blockX = blockPos.getX() - effectiveCenterVec.x;
            double blockY = blockPos.getY() - effectiveCenterVec.y;
            double blockZ = blockPos.getZ() - effectiveCenterVec.z;
            
            renderBlockBox(poseStack, buffer, blockX, blockY, blockZ, r, g, b);
        }
        
        renderCenterCrosshair(poseStack, buffer, effectiveCenterVec, minX, minY, minZ, maxX, maxY, maxZ);

        poseStack.popPose();
    }
    
    private boolean isOnShell(BlockPos pos, Set<BlockPos> allPositions) {
        return !allPositions.contains(pos.above()) ||
               !allPositions.contains(pos.below()) ||
               !allPositions.contains(pos.north()) ||
               !allPositions.contains(pos.south()) ||
               !allPositions.contains(pos.east()) ||
               !allPositions.contains(pos.west());
    }
    
    protected void renderCenterCrosshair(PoseStack poseStack, MultiBufferSource buffer, Vec3 effectiveCenter,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        VertexConsumer lines = buffer.getBuffer(LINES_SEE_THROUGH);
        PoseStack.Pose pose = poseStack.last();
        
        double trueCenterX = (minX + maxX + 1) / 2.0;
        double trueCenterY = (minY + maxY + 1) / 2.0;
        double trueCenterZ = (minZ + maxZ + 1) / 2.0;
        
        double cx = trueCenterX - effectiveCenter.x;
        double cy = trueCenterY - effectiveCenter.y;
        double cz = trueCenterZ - effectiveCenter.z;
        
        double x0 = minX - effectiveCenter.x;
        double x1 = maxX + 1 - effectiveCenter.x;
        double y0 = minY - effectiveCenter.y;
        double y1 = maxY + 1 - effectiveCenter.y;
        double z0 = minZ - effectiveCenter.z;
        double z1 = maxZ + 1 - effectiveCenter.z;
        
        float r = 1.0f, g = 1.0f, b = 1.0f, a = 0.9f;
        
        lines.addVertex(pose, (float) x0, (float) cy, (float) cz).setColor(r, g, b, a).setNormal(pose, 1, 0, 0);
        lines.addVertex(pose, (float) x1, (float) cy, (float) cz).setColor(r, g, b, a).setNormal(pose, 1, 0, 0);
        
        lines.addVertex(pose, (float) cx, (float) y0, (float) cz).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
        lines.addVertex(pose, (float) cx, (float) y1, (float) cz).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
        
        lines.addVertex(pose, (float) cx, (float) cy, (float) z0).setColor(r, g, b, a).setNormal(pose, 0, 0, 1);
        lines.addVertex(pose, (float) cx, (float) cy, (float) z1).setColor(r, g, b, a).setNormal(pose, 0, 0, 1);
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double camX, double camY, double camZ) {
        Vec3 entityPos = entity.position();
        double dx = entityPos.x - camX;
        double dy = entityPos.y - camY;
        double dz = entityPos.z - camZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        double maxDistanceSq = RENDER_DISTANCE * RENDER_DISTANCE;
        if (distanceSq > maxDistanceSq) {
            return false;
        }

        if (entity.isBuilding()) {
            AABB box = new AABB(entityPos.x - 2.0, entityPos.y - 2.0, entityPos.z - 2.0, entityPos.x + 2.0,
                    entityPos.y + 2.0, entityPos.z + 2.0);
            return frustum.isVisible(box);
        }

        int size = Math.max(1, entity.getSize());
        double pad = (size / 2.0) + 1.0;
        AABB box = new AABB(entityPos.x - pad, entityPos.y - pad, entityPos.z - pad, entityPos.x + pad,
                entityPos.y + pad, entityPos.z + pad);
        return frustum.isVisible(box);
    }

    protected void renderBlockBox(PoseStack poseStack, MultiBufferSource buffer, double x, double y, double z,
            float r, float g, float b) {
        VertexConsumer lines = buffer.getBuffer(LINES_SEE_THROUGH);
        renderLineBox(poseStack, lines, x, y, z, x + 1.0, y + 1.0, z + 1.0, r, g, b, LINE_ALPHA);
    }
    
    private static void renderLineBox(PoseStack poseStack, VertexConsumer consumer,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            float r, float g, float b, float a) {
        PoseStack.Pose pose = poseStack.last();
        float x0 = (float) minX;
        float y0 = (float) minY;
        float z0 = (float) minZ;
        float x1 = (float) maxX;
        float y1 = (float) maxY;
        float z1 = (float) maxZ;

        consumer.addVertex(pose, x0, y0, z0).setColor(r, g, b, a).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, x1, y0, z0).setColor(r, g, b, a).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, x0, y0, z0).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, x0, y1, z0).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, x0, y0, z0).setColor(r, g, b, a).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x0, y0, z1).setColor(r, g, b, a).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x1, y0, z0).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, x1, y1, z0).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, x1, y0, z0).setColor(r, g, b, a).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x1, y0, z1).setColor(r, g, b, a).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x0, y1, z0).setColor(r, g, b, a).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, x1, y1, z0).setColor(r, g, b, a).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, x0, y1, z0).setColor(r, g, b, a).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x0, y1, z1).setColor(r, g, b, a).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x0, y0, z1).setColor(r, g, b, a).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, x1, y0, z1).setColor(r, g, b, a).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, x0, y0, z1).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, x0, y1, z1).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, x1, y1, z0).setColor(r, g, b, a).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x1, y0, z1).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, x0, y1, z1).setColor(r, g, b, a).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, 1, 0, 0);
    }
}

