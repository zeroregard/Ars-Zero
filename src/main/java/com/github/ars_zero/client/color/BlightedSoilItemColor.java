package com.github.ars_zero.client.color;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlightedSoilItemColor implements ItemColor {

    public static final BlightedSoilItemColor INSTANCE = new BlightedSoilItemColor();

    // Midpoint of the in-world range: (92, 117, 77)
    private static final int ITEM_TINT = 0xFF000000 | (92 << 16) | (117 << 8) | 77;

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        return tintIndex == 0 ? ITEM_TINT : 0xFFFFFFFF;
    }
}
