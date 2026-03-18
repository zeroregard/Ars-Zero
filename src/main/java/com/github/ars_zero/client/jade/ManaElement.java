package com.github.ars_zero.client.jade;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IDisplayHelper;

public class ManaElement extends Element {

    private static final ResourceLocation SPRITE_CONTAINER =
            ResourceLocation.fromNamespaceAndPath("ars_zero", "mana_orb_container");
    private static final ResourceLocation SPRITE_FULL =
            ResourceLocation.fromNamespaceAndPath("ars_zero", "mana_orb_full");
    private static final ResourceLocation SPRITE_HALF =
            ResourceLocation.fromNamespaceAndPath("ars_zero", "mana_orb_half");

    private static final int MANA_PER_ICON = 100;
    private static final int MAX_ICONS = 20;
    private static final int ICONS_PER_LINE = 10;

    private final int current;
    private final int max;
    private final int iconCount;
    private final int iconsPerLine;
    private final int lineCount;
    private final int elemWidth;
    private final int elemHeight;

    public ManaElement(int current, int max) {
        this.current = current;
        this.max = max;

        int count = (int) Math.ceil(max / (double) MANA_PER_ICON);
        if (count > MAX_ICONS) {
            this.iconCount = 0;
            this.iconsPerLine = 0;
            this.lineCount = 0;
            this.elemWidth = 60;
            this.elemHeight = 9;
            message("Mana: " + current + "/" + max);
        } else {
            this.iconCount = Math.max(count, 1);
            this.iconsPerLine = Math.min(ICONS_PER_LINE, this.iconCount);
            this.lineCount = Mth.ceil((float) this.iconCount / ICONS_PER_LINE);
            this.elemWidth = 8 * iconsPerLine + 1;
            this.elemHeight = 5 + 4 * lineCount;
        }
    }

    @Override
    public Vec2 getSize() {
        return new Vec2(elemWidth, elemHeight);
    }

    @Override
    public void render(GuiGraphics graphics, float x, float y, float mouseX, float mouseY) {
        if (iconCount == 0) {
            IDisplayHelper.get().drawText(graphics, "Mana: " + current + "/" + max, x, y + 1, 0x9D4BDD);
            return;
        }

        IDisplayHelper helper = IDisplayHelper.get();
        int xOffset = (iconCount - 1) % iconsPerLine * 8;
        int yOffset = lineCount * 4 - 4;

        for (int i = iconCount; i > 0; --i) {
            int xPos = (int) x + xOffset;
            int yPos = (int) y + yOffset;

            helper.blitSprite(graphics, SPRITE_CONTAINER, xPos, yPos, 9, 9);

            int manaInSlot = current - (i - 1) * MANA_PER_ICON;
            if (manaInSlot >= MANA_PER_ICON) {
                helper.blitSprite(graphics, SPRITE_FULL, xPos, yPos, 9, 9);
            } else if (manaInSlot >= MANA_PER_ICON / 2) {
                helper.blitSprite(graphics, SPRITE_HALF, xPos, yPos, 9, 9);
            }

            xOffset -= 8;
            if (xOffset < 0) {
                xOffset = iconsPerLine * 8 - 8;
                yOffset -= 4;
            }
        }
    }
}
