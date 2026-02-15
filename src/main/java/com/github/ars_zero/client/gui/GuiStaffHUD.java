package com.github.ars_zero.client.gui;

import com.github.ars_zero.common.item.AbstractStaff;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.util.StackUtil;
import com.hollingsworth.arsnouveau.setup.config.Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public class GuiStaffHUD {
    public static final LayeredDraw.Layer OVERLAY = GuiStaffHUD::renderOverlay;

    private static final Minecraft minecraft = Minecraft.getInstance();

    public static void renderOverlay(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (Minecraft.getInstance().options.hideGui) {
            return;
        }
        
        ItemStack stack = StackUtil.getHeldCasterToolOrEmpty(minecraft.player);
        if (stack != ItemStack.EMPTY && stack.getItem() instanceof AbstractStaff) {
            int offsetLeft = Config.SPELLNAME_X_OFFSET.get();
            AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
            
            int logicalSlot = caster.getCurrentSlot();
            int[] physicalSlots = {
                logicalSlot * 3 + 0,
                logicalSlot * 3 + 1,
                logicalSlot * 3 + 2
            };
            
            String spellName = Arrays.stream(physicalSlots)
                .mapToObj(caster::getSpellName)
                .filter(name -> !name.isEmpty())
                .findFirst()
                .orElse("");
            
            String renderString = (logicalSlot + 1) + " " + spellName;
            
            graphics.drawString(
                Minecraft.getInstance().font, 
                renderString, 
                offsetLeft, 
                minecraft.getWindow().getGuiScaledHeight() - Config.SPELLNAME_Y_OFFSET.get(), 
                0xFFFFFF
            );
        }
    }
}

