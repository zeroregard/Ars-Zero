package com.github.ars_zero.client.renderer;

import com.github.ars_zero.common.entity.AbstractGeometryProcessEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class GeometryEntityOverlayHelper {
    public static final LayeredDraw.Layer OVERLAY = GeometryEntityOverlayHelper::renderOverlay;

    private static final boolean IS_MAC = Minecraft.ON_OSX;
    private static final Minecraft minecraft = Minecraft.getInstance();

    public static void renderOverlay(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (minecraft.options.hideGui) {
            return;
        }

        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        AbstractGeometryProcessEntity entity = findPlayerGeometryEntity(player);
        if (entity == null) {
            return;
        }

        // Don't show overlay when building/processing
        if (entity.isBuilding()) {
            return;
        }

        // Don't show overlay if lifespan has expired
        if (entity.getLifespan() <= 0) {
            return;
        }

        int size = entity.getSize();
        int depth = entity.getDepth();
        boolean isFlattened = entity.getGeometryDescription().isFlattened();

        String sizeModifierKey = IS_MAC ? "Cmd" : "Alt";
        String depthModifierKey = IS_MAC ? "Option" : "Ctrl";

        String text;
        if (isFlattened) {
            text = "Depth: " + depth + " (" + depthModifierKey + "+Scroll) - Size: " + size + " (" + sizeModifierKey + "+Scroll)";
        } else {
            text = "Size: " + size + " (" + sizeModifierKey + "+Scroll)";
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int textWidth = minecraft.font.width(text);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 104;

        graphics.drawString(minecraft.font, text, x, y, 0xFFFFFF);
    }

    private static AbstractGeometryProcessEntity findPlayerGeometryEntity(Player player) {
        if (minecraft.level == null) {
            return null;
        }

        UUID playerUuid = player.getUUID();
        
        // Search all entities in the level, not just by bounding box
        // (entity might be far from player with anchor)
        for (net.minecraft.world.entity.Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof AbstractGeometryProcessEntity geo) {
                UUID casterUuid = geo.getCasterUUID();
                if (casterUuid != null && casterUuid.equals(playerUuid)) {
                    return geo;
                }
            }
        }
        return null;
    }
}

