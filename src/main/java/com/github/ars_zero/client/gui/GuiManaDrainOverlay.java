package com.github.ars_zero.client.gui;

import com.hollingsworth.arsnouveau.api.ArsNouveauAPI;
import com.hollingsworth.arsnouveau.setup.config.Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

public class GuiManaDrainOverlay {

  public static final LayeredDraw.Layer OVERLAY = GuiManaDrainOverlay::renderOverlay;

  private static final Minecraft minecraft = Minecraft.getInstance();
  private static final int HIDE_AFTER_TICKS = 40;

  private static double lastDrainAmount = 0;
  private static int ticksSinceLastDrain = HIDE_AFTER_TICKS;

  public static void onManaDrain(double amount) {
    lastDrainAmount = amount;
    ticksSinceLastDrain = 0;
  }

  public static void tick() {
    if (ticksSinceLastDrain < HIDE_AFTER_TICKS) {
      ticksSinceLastDrain++;
    }
  }

  public static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
    if (!ArsNouveauAPI.ENABLE_DEBUG_NUMBERS) {
      return;
    }

    if (ticksSinceLastDrain >= HIDE_AFTER_TICKS) {
      return;
    }

    if (minecraft.player == null || Minecraft.getInstance().options.hideGui) {
      return;
    }

    int offsetLeft = 10 + Config.MANABAR_X_OFFSET.get();
    int yOffset = minecraft.getWindow().getGuiScaledHeight() - 5 + Config.MANABAR_Y_OFFSET.get();

    float alpha = 1.0f - ((float) ticksSinceLastDrain / HIDE_AFTER_TICKS);
    int alphaInt = (int) (alpha * 255) << 24;

    String drainText = String.format("-%.1f", lastDrainAmount);
    int textWidth = minecraft.font.width(drainText);

    int textX = offsetLeft + 70 - textWidth / 2;
    int textY = yOffset - 30;

    int color = 0xFF6666 | alphaInt;
    guiGraphics.drawString(minecraft.font, drainText, textX, textY, color, false);
  }
}
