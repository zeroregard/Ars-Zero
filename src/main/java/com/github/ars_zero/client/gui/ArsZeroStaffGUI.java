package com.github.ars_zero.client.gui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public class ArsZeroStaffGUI extends AbstractMultiPhaseCastDeviceScreen {

    public ArsZeroStaffGUI(ItemStack stack, InteractionHand hand) {
        super(stack, hand);
    }

    @Override
    protected ResourceLocation getBackgroundTexture() {
        DyeColor staffColor = DyeColor.PURPLE;
        if (deviceStack != null && !deviceStack.isEmpty() && deviceStack.has(DataComponents.BASE_COLOR)) {
            staffColor = deviceStack.get(DataComponents.BASE_COLOR);
        }
        return StaffGuiTextures.getBackgroundTexture(staffColor);
    }
}
