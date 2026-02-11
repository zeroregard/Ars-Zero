package com.github.ars_zero.common.item;

import com.github.ars_zero.common.spell.StaffSpellClipboard;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class MultiphaseSpellParchment extends Item {

    public MultiphaseSpellParchment(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return StaffSpellClipboard.readFromStack(stack, StaffSpellClipboard.PARCHMENT_SLOT_KEY)
            .filter(clip -> clip.name() != null && !clip.name().isBlank())
            .<Component>map(clip -> Component.literal(clip.name()))
            .orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        Optional<StaffSpellClipboard> clip = StaffSpellClipboard.readFromStack(stack, StaffSpellClipboard.PARCHMENT_SLOT_KEY);
        if (clip.isPresent()) {
            StaffSpellClipboard c = clip.get();
            if (c.name() != null && !c.name().isBlank()) {
                tooltip.add(Component.translatable("ars_zero.tooltip.multiphase_parchment.slot_name", c.name()));
            }
            tooltip.add(Component.translatable("ars_zero.tooltip.multiphase_parchment.tick_delay", c.tickDelay()));
        } else {
            tooltip.add(Component.translatable("ars_zero.tooltip.multiphase_parchment.empty"));
        }
        super.appendHoverText(stack, context, tooltip, flagIn);
    }
}
