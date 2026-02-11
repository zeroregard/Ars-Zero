package com.github.ars_zero.client.gui;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.network.CircletSlotInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class SpellcastingCircletGUI extends AbstractMultiPhaseCastDeviceScreen {

    private static final ResourceLocation BACKGROUND = ArsZero.prefix("textures/gui/circlet/background_default.png");
    private final CircletSlotInfo putBackSlot;

    public SpellcastingCircletGUI(ItemStack stack, InteractionHand hand) {
        this(stack, hand, null);
    }

    public SpellcastingCircletGUI(ItemStack stack, InteractionHand hand, CircletSlotInfo putBackSlot) {
        super(stack, hand);
        this.putBackSlot = putBackSlot;
    }

    public Optional<CircletSlotInfo> getPutBackSlot() {
        return Optional.ofNullable(putBackSlot);
    }

    @Override
    protected ResourceLocation getBackgroundTexture() {
        return BACKGROUND;
    }
}

