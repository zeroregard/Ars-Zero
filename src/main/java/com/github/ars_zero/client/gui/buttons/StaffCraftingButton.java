package com.github.ars_zero.client.gui.buttons;

import com.hollingsworth.arsnouveau.client.gui.buttons.CraftingButton;
import com.hollingsworth.arsnouveau.client.gui.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

public class StaffCraftingButton extends CraftingButton {
    
    public StaffCraftingButton(int x, int y, Button.OnPress onPress, int slotNum) {
        super(x, y, onPress, slotNum);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (validationErrors.isEmpty()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            RenderSystem.setShaderColor(1.0F, 0.7F, 0.7F, 1.0F);
        }
        if (this.abstractSpellPart != null) {
            RenderUtils.drawSpellPart(this.abstractSpellPart, graphics, getX() + 3, getY() + 2, 16, !validationErrors.isEmpty(), 0);
        }
    }
}

