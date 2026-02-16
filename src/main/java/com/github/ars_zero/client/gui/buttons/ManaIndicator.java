package com.github.ars_zero.client.gui.buttons;

import com.github.ars_zero.client.gui.PhaseManaHelper;
import com.github.ars_zero.common.item.AbstractStaticSpellStaff;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ManaIndicator {
    private static final int INDICATOR_WIDTH = 4;
    private static final int INDICATOR_HEIGHT = 14;
    private static final int MANA_COLOR = 0xFFC67EDE;

    private final int x;
    private final int y;
    private final List<AbstractSpellPart> phaseSpell;
    private final ItemStack deviceStack;

    /** No discount (e.g. regular staff). */
    public ManaIndicator(int x, int y, List<AbstractSpellPart> phaseSpell) {
        this(x, y, phaseSpell, ItemStack.EMPTY);
    }

    /** With optional device for discount (e.g. wand with IManaDiscountEquipment). */
    public ManaIndicator(int x, int y, List<AbstractSpellPart> phaseSpell, ItemStack deviceStack) {
        this.x = x;
        this.y = y;
        this.phaseSpell = phaseSpell;
        this.deviceStack = deviceStack != null ? deviceStack : ItemStack.EMPTY;
    }
    
    public void render(GuiGraphics graphics, Player player) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        
        boolean inBounds = x >= 0 && x < screenWidth && y >= 0 && y < screenHeight;
        
        IManaCap manaCap = CapabilityRegistry.getMana(player);
        if (manaCap == null) {
            graphics.fill(x - 1, y - 1, x + INDICATOR_WIDTH + 1, y + INDICATOR_HEIGHT + 1, 0xFFFF0000);
            graphics.fill(x, y, x + INDICATOR_WIDTH, y + INDICATOR_HEIGHT, 0x33FF0000);
            return;
        }
        
        double maxMana = manaCap.getMaxMana();
        if (maxMana <= 0) {
            graphics.fill(x - 1, y - 1, x + INDICATOR_WIDTH + 1, y + INDICATOR_HEIGHT + 1, 0xFFFF0000);
            graphics.fill(x, y, x + INDICATOR_WIDTH, y + INDICATOR_HEIGHT, 0x33FF0000);
            return;
        }
        
        int spellCost = PhaseManaHelper.getDisplayCost(phaseSpell, deviceStack);
        double fillRatio = Math.min(spellCost / maxMana, 1.0);
        int fillHeight = (int) (INDICATOR_HEIGHT * fillRatio);
        
        if (spellCost > 0 && fillHeight == 0) {
            fillHeight = 1;
        }
        
        if (fillHeight > 0) {
            int fillY = y + INDICATOR_HEIGHT - fillHeight;
            graphics.fill(x, fillY, x + INDICATOR_WIDTH, y + INDICATOR_HEIGHT, MANA_COLOR);
        } else {
            graphics.fill(x, y, x + INDICATOR_WIDTH, y + INDICATOR_HEIGHT, 0x33C67EDE);
        }
    }
    
    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + INDICATOR_WIDTH &&
               mouseY >= y && mouseY < y + INDICATOR_HEIGHT;
    }
    
    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int displayCost = PhaseManaHelper.getDisplayCost(phaseSpell, deviceStack);
        int rawCost = PhaseManaHelper.getRawCost(phaseSpell);
        boolean hasDiscount = !deviceStack.isEmpty() && rawCost > 0 && displayCost < rawCost;
        int discountPercent = 0;
        if (hasDiscount) {
            if (deviceStack.getItem() instanceof AbstractStaticSpellStaff staff) {
                discountPercent = staff.getDiscountPercent();
            }
            if (discountPercent <= 0) {
                discountPercent = (int) Math.round(100.0 * (rawCost - displayCost) / rawCost);
            }
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        IManaCap manaCap = CapabilityRegistry.getMana(player);
        if (manaCap == null) {
            Component tooltip = buildManaLine(displayCost, hasDiscount, discountPercent, false);
            graphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
            return;
        }

        double maxMana = manaCap.getMaxMana();
        boolean exceedsMax = displayCost > maxMana;
        Component tooltip = buildManaLine(displayCost, hasDiscount, discountPercent, exceedsMax);
        graphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
    }

    private Component buildManaLine(int displayCost, boolean hasDiscount, int discountPercent, boolean exceedsMax) {
        MutableComponent line = Component.literal("~" + displayCost + " mana");
        if (hasDiscount && discountPercent > 0) {
            line = line.append(Component.literal(" (" + discountPercent + "% discount)").withStyle(ChatFormatting.GRAY));
        }
        if (exceedsMax) {
            line = line.append(Component.literal(" - exceeds your max mana level").withStyle(ChatFormatting.RED));
        }
        return line;
    }
}

