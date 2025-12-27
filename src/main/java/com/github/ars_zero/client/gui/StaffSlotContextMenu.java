package com.github.ars_zero.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Objects;

public final class StaffSlotContextMenu {

    private final Font font;
    private int x;
    private int y;
    private boolean visible;
    private boolean pasteEnabled;
    private Runnable onCopy;
    private Runnable onPaste;

    public StaffSlotContextMenu(Font font) {
        this.font = Objects.requireNonNull(font);
    }

    public boolean isVisible() {
        return visible;
    }

    public void hide() {
        visible = false;
        onCopy = null;
        onPaste = null;
    }

    public void show(int x, int y, boolean pasteEnabled, Runnable onCopy, Runnable onPaste) {
        this.x = x;
        this.y = y;
        this.pasteEnabled = pasteEnabled;
        this.onCopy = onCopy;
        this.onPaste = onPaste;
        this.visible = true;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500);

        int width = Math.max(textWidth("Copy"), textWidth("Paste")) + 12;
        int rowHeight = 12;
        int height = rowHeight * 2 + 4;

        graphics.fill(x, y, x + width, y + height, 0xCC000000);
        graphics.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + height, 0xFFFFFFFF);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);

        int copyY = y + 2;
        int pasteY = y + 2 + rowHeight;

        graphics.fill(x + 1, pasteY - 1, x + width - 1, pasteY, 0xFFFFFFFF);

        boolean hoverCopy = isOverRow(mouseX, mouseY, copyY, rowHeight, width);
        boolean hoverPaste = isOverRow(mouseX, mouseY, pasteY, rowHeight, width);

        if (hoverCopy) {
            graphics.fill(x + 1, copyY, x + width - 1, copyY + rowHeight, 0x66000000);
        }
        if (hoverPaste && pasteEnabled) {
            graphics.fill(x + 1, pasteY, x + width - 1, pasteY + rowHeight, 0x66000000);
        }

        graphics.drawString(font, "Copy", x + 6, copyY + 2, 0xFFFFFF, false);
        int pasteColor = pasteEnabled ? 0xFFFFFF : 0x888888;
        graphics.drawString(font, "Paste", x + 6, pasteY + 2, pasteColor, false);

        graphics.pose().popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }
        if (button != 0 && button != 1) {
            return false;
        }
        int width = Math.max(textWidth("Copy"), textWidth("Paste")) + 12;
        int rowHeight = 12;
        int height = rowHeight * 2 + 4;
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (mx < x || my < y || mx >= x + width || my >= y + height) {
            return false;
        }

        int copyY = y + 2;
        int pasteY = y + 2 + rowHeight;

        if (my >= copyY && my < copyY + rowHeight) {
            if (onCopy != null) {
                onCopy.run();
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            hide();
            return true;
        }

        if (my >= pasteY && my < pasteY + rowHeight) {
            if (pasteEnabled && onPaste != null) {
                onPaste.run();
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                hide();
                return true;
            }
            return true;
        }

        return true;
    }

    private boolean isOverRow(int mouseX, int mouseY, int rowY, int rowHeight, int width) {
        return mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + rowHeight;
    }

    private int textWidth(String text) {
        return font.width(text);
    }
}


