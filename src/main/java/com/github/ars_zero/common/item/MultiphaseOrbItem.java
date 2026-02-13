package com.github.ars_zero.common.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MultiphaseOrbItem extends Item {

    public MultiphaseOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copyWithCount(1);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
