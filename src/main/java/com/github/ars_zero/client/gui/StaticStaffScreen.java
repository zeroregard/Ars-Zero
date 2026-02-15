package com.github.ars_zero.client.gui;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.gui.buttons.ManaIndicator;
import com.github.ars_zero.client.gui.documentation.GlyphDocTooltipHelper;
import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.spell.SpellPhase;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.client.gui.utils.RenderUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StaticStaffScreen extends Screen {

    // Background centered on screen; content origin (bookLeft, bookTop) = top-left of bg
    private static final int BG_WIDTH = 278;
    private static final int BG_HEIGHT = 90;
    private static final ResourceLocation STATIC_STAFF_BG = ArsZero.prefix("textures/gui/staff/static_staff_screen.png");

    // Layout fitted to 278x90 bg: phase section and spell name inside parchment
    private static final int STAFF_GUI_WIDTH = 375;
    private static final int STAFF_GUI_HEIGHT = 232;
    private static final int PHASE_ROW_TEXTURE_WIDTH = 253;
    private static final int PHASE_ROW_TEXTURE_HEIGHT = 20;
    private static final int PHASE_ROW_TEXTURE_X_OFFSET = 24;
    private static final int PHASE_SECTION_Y_OFFSET = 18;
    private static final int PHASE_ROW_HEIGHT = 20;
    private static final int CRAFTING_CELL_START_X_OFFSET = 36;
    private static final int CRAFTING_CELL_SPACING = 24;
    private static final int PHASE_SECTION_SHIFT_X = 5;
    private static final int PHASE_SECTION_SHIFT_Y = 0;
    private static final int SPELL_NAME_X_OFFSET = 12;
    private static final int SPELL_NAME_Y_OFFSET = 6;
    private static final int CELL_WIDTH = 22;
    private static final int CELL_HEIGHT = 20;
    private static final int GLYPH_INSET_X = 3;
    private static final int GLYPH_INSET_Y = 2;
    private static final int GLYPH_SIZE = 16;
    private static final int MAX_GLYPH_CELLS = 10;

    protected final ItemStack deviceStack;
    protected final InteractionHand guiHand;
    private int bookLeft;
    private int bookTop;
    @Nullable
    private com.hollingsworth.arsnouveau.api.spell.AbstractCaster<?> caster;

    /** For tooltip: (x, y, w, h, part) of each drawn glyph. */
    private final List<GlyphHitArea> glyphHitAreas = new ArrayList<>();
    /** Phase icon bounds for tooltips: (x, y, w, h, phase). */
    private final List<PhaseIconHitArea> phaseIconHitAreas = new ArrayList<>();

    /** So we only draw the parchment once per frame; super.render() calls renderBackground() again and would otherwise paint over content. */
    private boolean backgroundDrawnThisFrame;

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        backgroundDrawnThisFrame = false;
        renderBackground(graphics, mouseX, mouseY, partialTick);
        glyphHitAreas.clear();
        phaseIconHitAreas.clear();
        refreshCaster();

        if (caster == null) {
            graphics.drawCenteredString(font, Component.translatable("gui.ars_zero.static_staff.no_data").getString(), width / 2, bookTop + BG_HEIGHT / 2 - 4, 0xFF5555);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        drawPhaseSection(graphics, mouseX, mouseY);
        renderManaIndicators(graphics, mouseX, mouseY);
        renderGlyphTooltip(graphics, mouseX, mouseY);
        renderPhaseIconTooltip(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public StaticStaffScreen(ItemStack stack, InteractionHand hand) {
        super(Component.translatable("gui.ars_zero.static_staff_title"));
        this.deviceStack = stack;
        this.guiHand = hand;
    }

    @Override
    protected void init() {
        super.init();
        // Center the 278x90 bg; content origin = top-left of bg so all elements move together
        bookLeft = width / 2 - BG_WIDTH / 2;
        bookTop = height / 2 - BG_HEIGHT / 2;
        refreshCaster();
    }

    private void refreshCaster() {
        Player player = minecraft != null ? minecraft.player : null;
        if (player == null) {
            caster = null;
            return;
        }
        ItemStack stack = ItemStack.EMPTY;
        if (guiHand != null) {
            stack = player.getItemInHand(guiHand);
        }
        if (stack.isEmpty()) {
            stack = player.getMainHandItem();
        }
        if (stack.isEmpty()) {
            stack = player.getOffhandItem();
        }
        if (!stack.isEmpty() && stack.getItem() == deviceStack.getItem()) {
            caster = SpellCasterRegistry.from(stack);
        } else {
            caster = SpellCasterRegistry.from(deviceStack);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Only draw once per frame; super.render() calls this again and would otherwise draw parchment on top of content
        if (backgroundDrawnThisFrame) {
            return;
        }
        backgroundDrawnThisFrame = true;
        // Light overlay so the GUI is readable without a heavy backdrop.
        graphics.fill(0, 0, width, height, 0x40000000);
        // Centered parchment background (278x90)
        graphics.blit(STATIC_STAFF_BG, bookLeft, bookTop, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);
    }

    private void drawPhaseSection(GuiGraphics graphics, int mouseX, int mouseY) {
        int rowX = bookLeft + PHASE_ROW_TEXTURE_X_OFFSET + PHASE_SECTION_SHIFT_X - 8;
        int baseRowY = bookTop + PHASE_SECTION_Y_OFFSET + PHASE_SECTION_SHIFT_Y;
        int rowHeight = PHASE_ROW_HEIGHT + 2;

        int phaseIconX = bookLeft + PHASE_SECTION_SHIFT_X - 2;
        int phaseIconY = bookTop + PHASE_SECTION_Y_OFFSET + PHASE_SECTION_SHIFT_Y + 2;

        int cellStartX = bookLeft + CRAFTING_CELL_START_X_OFFSET + PHASE_SECTION_SHIFT_X - 21;
        int cellStartY = bookTop + PHASE_SECTION_Y_OFFSET + PHASE_SECTION_SHIFT_Y;

        for (int i = 0; i < SpellPhase.values().length; i++) {
            SpellPhase phase = SpellPhase.values()[i];
            int physicalSlot = phase.ordinal();
            int rowY = baseRowY + i * rowHeight;

            ResourceLocation rowTexture = StaffGuiTextures.SPELL_PHASE_ROW;
            graphics.blit(rowTexture, rowX, rowY, 0, 0, PHASE_ROW_TEXTURE_WIDTH, PHASE_ROW_TEXTURE_HEIGHT,
                    PHASE_ROW_TEXTURE_WIDTH, PHASE_ROW_TEXTURE_HEIGHT);

            ResourceLocation icon = phase == SpellPhase.BEGIN ? StaffGuiTextures.ICON_START
                    : phase == SpellPhase.TICK ? StaffGuiTextures.ICON_TICK
                    : StaffGuiTextures.ICON_END;
            int iconY = phaseIconY + i * rowHeight;
            graphics.blit(icon, phaseIconX, iconY, 0, 0, 16, 16, 16, 16);
            phaseIconHitAreas.add(new PhaseIconHitArea(phaseIconX, iconY, 16, 16, phase));

            Spell spell = caster.getSpell(physicalSlot);
            List<AbstractSpellPart> recipe = new ArrayList<>();
            if (spell != null && !spell.isEmpty()) {
                for (AbstractSpellPart part : spell.recipe()) {
                    recipe.add(part);
                }
            }

            for (int slot = 0; slot < MAX_GLYPH_CELLS; slot++) {
                int cellX = cellStartX + slot * CRAFTING_CELL_SPACING;
                int cellY = cellStartY + i * rowHeight;

                if (slot < recipe.size()) {
                    AbstractSpellPart part = recipe.get(slot);
                    int gx = cellX + GLYPH_INSET_X;
                    int gy = cellY + GLYPH_INSET_Y;
                    RenderUtils.drawSpellPart(part, graphics, gx, gy, GLYPH_SIZE, false, 0);
                    glyphHitAreas.add(new GlyphHitArea(gx, gy, GLYPH_SIZE, GLYPH_SIZE, part));
                }
            }
        }

        String spellName = caster.getSpellName(0);
        if (spellName != null && !spellName.isEmpty()) {
            graphics.drawString(font, spellName, bookLeft + SPELL_NAME_X_OFFSET, bookTop + SPELL_NAME_Y_OFFSET, 0x404040, false);
        }
    }

    private void renderGlyphTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Player player = minecraft != null ? minecraft.player : null;
        for (GlyphHitArea area : glyphHitAreas) {
            if (mouseX >= area.x && mouseX < area.x + area.w && mouseY >= area.y && mouseY < area.y + area.h) {
                List<Component> tooltip = GlyphDocTooltipHelper.getTooltipLinesForSpellPart(area.part, player, Screen.hasShiftDown());
                if (!tooltip.isEmpty()) {
                    graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
                }
                return;
            }
        }
    }

    private void renderManaIndicators(GuiGraphics graphics, int mouseX, int mouseY) {
        int rowX = bookLeft + PHASE_ROW_TEXTURE_X_OFFSET + PHASE_SECTION_SHIFT_X - 8;
        int phaseRowEndX = rowX + PHASE_ROW_TEXTURE_WIDTH;
        int indicatorX = phaseRowEndX + 4 - 8 - 4 + 1;
        int baseY = bookTop + PHASE_SECTION_Y_OFFSET + PHASE_SECTION_SHIFT_Y;
        int rowHeight = PHASE_ROW_HEIGHT + 2;
        int indicatorHeight = 14;

        Player player = minecraft != null ? minecraft.player : null;
        if (player == null) {
            return;
        }

        ManaIndicator hoveredIndicator = null;
        int phaseIndex = 0;
        for (SpellPhase phase : SpellPhase.values()) {
            int physicalSlot = phase.ordinal();
            Spell spell = caster.getSpell(physicalSlot);
            List<AbstractSpellPart> phaseSpell = new ArrayList<>();
            if (spell != null && !spell.isEmpty()) {
                for (AbstractSpellPart part : spell.recipe()) {
                    phaseSpell.add(part);
                }
            }
            int indicatorY = baseY + phaseIndex * rowHeight + (PHASE_ROW_HEIGHT - indicatorHeight) / 2 - 1 + 1;

            ManaIndicator indicator = new ManaIndicator(indicatorX, indicatorY, phaseSpell, deviceStack);
            indicator.render(graphics, player);

            if (indicator.isHovered(mouseX, mouseY)) {
                hoveredIndicator = indicator;
            }
            phaseIndex++;
        }

        if (hoveredIndicator != null) {
            hoveredIndicator.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private void renderPhaseIconTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (PhaseIconHitArea area : phaseIconHitAreas) {
            if (mouseX >= area.x && mouseX < area.x + area.w && mouseY >= area.y && mouseY < area.y + area.h) {
                List<Component> tooltip = new ArrayList<>();
                if (area.phase == SpellPhase.BEGIN) {
                    tooltip.add(Component.translatable("gui.ars_zero.phase.begin.tooltip"));
                } else if (area.phase == SpellPhase.TICK) {
                    tooltip.add(Component.translatable("gui.ars_zero.phase.tick.tooltip"));
                    int delay = AbstractMultiPhaseCastDevice.getSlotTickDelay(deviceStack, 0);
                    tooltip.add(Component.translatable("gui.ars_zero.static_staff.tick_delay", delay)
                            .copy().setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
                } else {
                    tooltip.add(Component.translatable("gui.ars_zero.phase.end.tooltip"));
                }
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record GlyphHitArea(int x, int y, int w, int h, AbstractSpellPart part) {}
    private record PhaseIconHitArea(int x, int y, int w, int h, SpellPhase phase) {}
}
