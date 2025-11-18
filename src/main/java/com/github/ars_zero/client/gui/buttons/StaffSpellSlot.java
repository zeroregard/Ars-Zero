package com.github.ars_zero.client.gui.buttons;

import com.github.ars_zero.client.gui.StaffGuiTextures;
import com.hollingsworth.arsnouveau.client.gui.buttons.SelectableButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class StaffSpellSlot extends SelectableButton {
    public int slotNum;
    public String spellName;

    public StaffSpellSlot(int x, int y, int slotNum, String spellName, OnPress onPress) {
        super(x, y, 0, 0, 14, 14, 14, 14, StaffGuiTextures.ICON_SLOT_SELECTED, StaffGuiTextures.ICON_SLOT_SELECTED, onPress);
        this.slotNum = slotNum;
        this.spellName = spellName;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (isSelected) {
            this.image = StaffGuiTextures.ICON_SLOT_SELECTED;
            super.renderWidget(graphics, pMouseX, pMouseY, pPartialTick);
        }
        graphics.drawCenteredString(Minecraft.getInstance().font, String.valueOf(this.slotNum + 1), getX() + 7, getY() + 3, 16777215);
    }

    @Override
    public void getTooltip(List<Component> tooltip) {
        if (!spellName.isEmpty()) {
            tooltip.add(Component.literal(spellName));
        }
    }
}

