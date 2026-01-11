package com.github.ars_zero.client.renderer;

import com.github.ars_zero.common.entity.terrain.ConjureTerrainConvergenceEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

public class ConjureTerrainSizeOverlayHelper {
  public static final LayeredDraw.Layer OVERLAY = ConjureTerrainSizeOverlayHelper::renderOverlay;

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

    ConjureTerrainConvergenceEntity entity = findPlayerTerrainEntity(player);
    if (entity == null) {
      return;
    }

    if (entity.isBuilding()) {
      return;
    }

    if (entity.getLifespan() <= 0) {
      return;
    }

    int size = entity.getSize();
    String modifierKey = IS_MAC ? "Cmd" : "Alt";
    String text = "Size: " + size + " (" + modifierKey + "+Scroll)";

    int screenWidth = minecraft.getWindow().getGuiScaledWidth();
    int screenHeight = minecraft.getWindow().getGuiScaledHeight();
    int textWidth = minecraft.font.width(text);
    int x = (screenWidth - textWidth) / 2;
    int y = screenHeight - 40;

    graphics.drawString(minecraft.font, text, x, y, 0xFFFFFF);
  }

  private static ConjureTerrainConvergenceEntity findPlayerTerrainEntity(Player player) {
    if (minecraft.level == null) {
      return null;
    }

    java.util.UUID playerUuid = player.getUUID();
    return minecraft.level.getEntitiesOfClass(
        ConjureTerrainConvergenceEntity.class,
        player.getBoundingBox().inflate(128.0),
        entity -> {
          java.util.UUID casterUuid = entity.getCasterUUID();
          return casterUuid != null && casterUuid.equals(playerUuid);
        })
        .stream()
        .findFirst()
        .orElse(null);
  }
}
