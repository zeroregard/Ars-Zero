package com.github.ars_zero.client;

import com.github.ars_zero.common.item.SpellcastingCirclet;
import net.minecraft.world.item.ItemStack;

public final class RadialMenuTracker {

    private static ItemStack activeStack = ItemStack.EMPTY;

    private RadialMenuTracker() {
    }

    public static void activate(ItemStack stack) {
        activeStack = stack.copy();
    }

    public static void clear() {
        activeStack = ItemStack.EMPTY;
    }

    public static ItemStack getActiveStack() {
        return activeStack;
    }

    public static boolean isCircletActive() {
        return !activeStack.isEmpty() && activeStack.getItem() instanceof SpellcastingCirclet;
    }
}

