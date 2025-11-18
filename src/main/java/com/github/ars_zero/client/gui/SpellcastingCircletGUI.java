package com.github.ars_zero.client.gui;

import com.github.ars_zero.ArsZero;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class SpellcastingCircletGUI extends AbstractMultiPhaseCastDeviceScreen {

    private static final ResourceLocation BACKGROUND = ArsZero.prefix("textures/gui/circlet/background_default.png");

    public SpellcastingCircletGUI(ItemStack stack, InteractionHand hand) {
        super(stack, hand);
    }

    @Override
    protected ResourceLocation getBackgroundTexture() {
        return BACKGROUND;
    }
}

