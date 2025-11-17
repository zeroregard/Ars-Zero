package com.github.ars_zero.client.gui.buttons;

import com.github.ars_zero.client.gui.StaffGuiTextures;
import com.hollingsworth.arsnouveau.client.gui.buttons.GuiImageButton;
import net.minecraft.client.gui.components.Button;

public class StaffArrowButton extends GuiImageButton {
    
    public StaffArrowButton(int x, int y, boolean isRight, Button.OnPress onPress) {
        super(x, y, 8, 10, 
            isRight ? StaffGuiTextures.ARROW_RIGHT : StaffGuiTextures.ARROW_LEFT, 
            onPress);
        this.hoverImage = isRight ? StaffGuiTextures.ARROW_RIGHT_HOVER : StaffGuiTextures.ARROW_LEFT_HOVER;
    }
}

