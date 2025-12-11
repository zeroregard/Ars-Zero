package com.github.ars_zero.client.gui.buttons;

import com.github.ars_zero.ArsZero;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class ManaIndicator {
    private static final int INDICATOR_WIDTH = 4;
    private static final int INDICATOR_HEIGHT = 14;
    private static final int MANA_COLOR = 0xFFC67EDE;
    
    private final int x;
    private final int y;
    private final List<AbstractSpellPart> phaseSpell;
    
    public ManaIndicator(int x, int y, List<AbstractSpellPart> phaseSpell) {
        this.x = x;
        this.y = y;
        this.phaseSpell = phaseSpell;
        ArsZero.LOGGER.debug("ManaIndicator created: x={}, y={}, spell parts={}", x, y, phaseSpell.size());
    }
    
    public void render(GuiGraphics graphics, Player player) {
        ArsZero.LOGGER.debug("ManaIndicator.render() called: x={}, y={}, player={}", x, y, player != null ? player.getName().getString() : "NULL");
        
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        ArsZero.LOGGER.debug("ManaIndicator: Screen dimensions: {}x{}", screenWidth, screenHeight);
        ArsZero.LOGGER.debug("ManaIndicator: Indicator bounds: x=[{}, {}], y=[{}, {}]", x, x + INDICATOR_WIDTH, y, y + INDICATOR_HEIGHT);
        
        boolean inBounds = x >= 0 && x < screenWidth && y >= 0 && y < screenHeight;
        ArsZero.LOGGER.debug("ManaIndicator: In screen bounds: {}", inBounds);
        
        IManaCap manaCap = CapabilityRegistry.getMana(player);
        if (manaCap == null) {
            ArsZero.LOGGER.warn("ManaIndicator: manaCap is NULL! Drawing debug indicator anyway");
            graphics.fill(x - 1, y - 1, x + INDICATOR_WIDTH + 1, y + INDICATOR_HEIGHT + 1, 0xFFFF0000);
            graphics.fill(x, y, x + INDICATOR_WIDTH, y + INDICATOR_HEIGHT, 0x33FF0000);
            return;
        }
        
        double maxMana = manaCap.getMaxMana();
        ArsZero.LOGGER.debug("ManaIndicator: maxMana={}", maxMana);
        if (maxMana <= 0) {
            ArsZero.LOGGER.warn("ManaIndicator: maxMana <= 0, drawing debug indicator");
            graphics.fill(x - 1, y - 1, x + INDICATOR_WIDTH + 1, y + INDICATOR_HEIGHT + 1, 0xFFFF0000);
            graphics.fill(x, y, x + INDICATOR_WIDTH, y + INDICATOR_HEIGHT, 0x33FF0000);
            return;
        }
        
        int spellCost = calculatePhaseManaCost();
        ArsZero.LOGGER.debug("ManaIndicator: spellCost={}", spellCost);
        double fillRatio = Math.min(spellCost / maxMana, 1.0);
        int fillHeight = (int) (INDICATOR_HEIGHT * fillRatio);
        
        if (spellCost > 0 && fillHeight == 0) {
            fillHeight = 1;
        }
        
        ArsZero.LOGGER.debug("ManaIndicator: fillRatio={}, fillHeight={}", fillRatio, fillHeight);
        
        if (fillHeight > 0) {
            int fillY = y + INDICATOR_HEIGHT - fillHeight;
            ArsZero.LOGGER.debug("ManaIndicator: Drawing fill at x={}, y={} to x={}, y={}, color=0x{:08X}", x, fillY, x + INDICATOR_WIDTH, y + INDICATOR_HEIGHT, MANA_COLOR);
            graphics.fill(x, fillY, x + INDICATOR_WIDTH, y + INDICATOR_HEIGHT, MANA_COLOR);
            ArsZero.LOGGER.debug("ManaIndicator: Fill drawn successfully");
        } else {
            ArsZero.LOGGER.warn("ManaIndicator: fillHeight is 0, drawing empty indicator");
            graphics.fill(x, y, x + INDICATOR_WIDTH, y + INDICATOR_HEIGHT, 0x33C67EDE);
        }
        
        ArsZero.LOGGER.debug("ManaIndicator.render() completed");
    }
    
    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + INDICATOR_WIDTH &&
               mouseY >= y && mouseY < y + INDICATOR_HEIGHT;
    }
    
    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int spellCost = calculatePhaseManaCost();
        Player player = Minecraft.getInstance().player;
        
        if (player == null) {
            return;
        }
        
        IManaCap manaCap = CapabilityRegistry.getMana(player);
        if (manaCap == null) {
            Component tooltip = Component.literal("~" + spellCost + " mana");
            graphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
            return;
        }
        
        double maxMana = manaCap.getMaxMana();
        Component tooltip;
        
        if (spellCost > maxMana) {
            tooltip = Component.literal("~" + spellCost + " mana").append(
                Component.literal(" - exceeds your max mana level").withStyle(ChatFormatting.RED)
            );
        } else {
            tooltip = Component.literal("~" + spellCost + " mana");
        }
        
        graphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
    }
    
    private int calculatePhaseManaCost() {
        List<AbstractSpellPart> filteredSpell = new ArrayList<>();
        for (AbstractSpellPart part : phaseSpell) {
            if (part != null) {
                filteredSpell.add(part);
            }
        }
        
        Spell spell = new Spell(filteredSpell);
        return spell.getCost();
    }
}

