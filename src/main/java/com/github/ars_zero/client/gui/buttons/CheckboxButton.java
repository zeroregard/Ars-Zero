package com.github.ars_zero.client.gui.buttons;

import com.hollingsworth.arsnouveau.api.documentation.DocAssets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

public class CheckboxButton extends com.hollingsworth.arsnouveau.client.gui.buttons.SelectableButton {
    
    public CheckboxButton(int x, int y, Button.OnPress onPress) {
        super(x, y, DocAssets.SPELLSTYLE_FRAME, DocAssets.SPELLSTYLE_FRAME, onPress);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderWidget(graphics, pMouseX, pMouseY, pPartialTick);
        
        if (isSelected) {
            int x = getX() + 3;
            int y = getY() + 3;
            int size = 8;
            graphics.fill(x, y, x + size, y + size, 0xFF1A1A1A);
        }
    }
}
