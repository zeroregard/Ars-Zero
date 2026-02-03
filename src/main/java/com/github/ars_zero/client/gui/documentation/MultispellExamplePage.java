package com.github.ars_zero.client.gui.documentation;

import com.github.ars_zero.client.gui.StaffGuiTextures;
import com.hollingsworth.arsnouveau.api.documentation.DocClientUtils;
import com.hollingsworth.arsnouveau.api.documentation.SinglePageCtor;
import com.hollingsworth.arsnouveau.api.documentation.SinglePageWidget;
import com.hollingsworth.arsnouveau.client.gui.documentation.BaseDocScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MultispellExamplePage extends SinglePageWidget {

    private static final float GLYPH_SCALE = 0.5f;
    private static final int SLOT_SIZE_FULL = 16;
    private static final int PADDING = 2;
    private static final int GAP_PHASE_TO_GLYPHS = 2 * PADDING;
    private static final int ICON_SIZE = 16;
    private static final int ICON_SIZE_SCALED = (int) (ICON_SIZE * GLYPH_SCALE);
    private static final int PHASE_ICON_WIDTH = ICON_SIZE_SCALED + PADDING + GAP_PHASE_TO_GLYPHS;
    private static final int ROW_HEIGHT = 11;
    private static final int TITLE_HEIGHT = 14;
    private static final int GLYPH_BLOCK_HEIGHT = ROW_HEIGHT * 3;
    private static final int QUOTE_TOP_PADDING = 6;

    private final Component title;
    private final Component quote;
    private final List<ItemStack> beginGlyphs;
    private final List<ItemStack> tickGlyphs;
    private final List<ItemStack> endGlyphs;
    private final Component beginPhaseTooltip;
    private final Component tickPhaseTooltip;
    private final Component endPhaseTooltip;

    private Component phaseTooltip;

    public MultispellExamplePage(Component title, Component quote,
                                 List<ItemStack> beginGlyphs, List<ItemStack> tickGlyphs, List<ItemStack> endGlyphs,
                                 Component beginPhaseTooltip, Component tickPhaseTooltip, Component endPhaseTooltip,
                                 BaseDocScreen parent, int x, int y, int width, int height) {
        super(parent, x, y, width, height);
        this.title = title;
        this.quote = quote;
        this.beginGlyphs = beginGlyphs != null ? beginGlyphs : List.of();
        this.tickGlyphs = tickGlyphs != null ? tickGlyphs : List.of();
        this.endGlyphs = endGlyphs != null ? endGlyphs : List.of();
        this.beginPhaseTooltip = beginPhaseTooltip;
        this.tickPhaseTooltip = tickPhaseTooltip;
        this.endPhaseTooltip = endPhaseTooltip;
    }

    public static SinglePageCtor create(Component title, Component quote,
                                       List<ItemStack> beginGlyphs, List<ItemStack> tickGlyphs, List<ItemStack> endGlyphs,
                                       Component beginPhaseTooltip, Component tickPhaseTooltip, Component endPhaseTooltip) {
        return (parent, x, y, width, height) -> new MultispellExamplePage(
                title, quote, beginGlyphs, tickGlyphs, endGlyphs,
                beginPhaseTooltip, tickPhaseTooltip, endPhaseTooltip,
                parent, x, y, width, height);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        phaseTooltip = null;
        int baseX = getX();
        int baseY = getY();

        DocClientUtils.drawHeaderNoUnderline(title, guiGraphics, baseX, baseY, getWidth(), mouseX, mouseY, partialTick);

        int glyphBlockY = baseY + TITLE_HEIGHT;
        int glyphStartX = baseX + PHASE_ICON_WIDTH;
        renderPhaseRow(guiGraphics, glyphBlockY, glyphStartX, StaffGuiTextures.ICON_START_SELECTED, beginPhaseTooltip, beginGlyphs, mouseX, mouseY);
        renderPhaseRow(guiGraphics, glyphBlockY + ROW_HEIGHT, glyphStartX, StaffGuiTextures.ICON_TICK_SELECTED, tickPhaseTooltip, tickGlyphs, mouseX, mouseY);
        renderPhaseRow(guiGraphics, glyphBlockY + ROW_HEIGHT * 2, glyphStartX, StaffGuiTextures.ICON_END_SELECTED, endPhaseTooltip, endGlyphs, mouseX, mouseY);

        int quoteY = glyphBlockY + GLYPH_BLOCK_HEIGHT + QUOTE_TOP_PADDING;
        DocClientUtils.drawParagraph(quote, guiGraphics, baseX, quoteY, getWidth(), mouseX, mouseY, partialTick);
    }

    private void renderPhaseRow(GuiGraphics guiGraphics, int rowY, int glyphStartX, ResourceLocation iconTexture,
                                Component phaseTooltipText, List<ItemStack> glyphs, int mouseX, int mouseY) {
        int iconX = getX() + 2;
        int iconY = rowY + 2;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(iconX, iconY, 0);
        guiGraphics.pose().scale(GLYPH_SCALE, GLYPH_SCALE, 1f);
        guiGraphics.blit(iconTexture, 0, 0, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        guiGraphics.pose().popPose();
        if (mouseX >= iconX && mouseX < iconX + ICON_SIZE_SCALED && mouseY >= iconY && mouseY < iconY + ICON_SIZE_SCALED) {
            phaseTooltip = phaseTooltipText;
        }

        if (glyphs == null || glyphs.isEmpty()) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(glyphStartX, rowY + 2, 0);
        guiGraphics.pose().scale(GLYPH_SCALE, GLYPH_SCALE, 1f);
        int col = 0;
        for (ItemStack stack : glyphs) {
            if (!stack.isEmpty()) {
                int renderX = col * (SLOT_SIZE_FULL + PADDING);
                int renderY = 0;
                int scaledMouseX = (int) ((mouseX - glyphStartX) / GLYPH_SCALE);
                int scaledMouseY = (int) ((mouseY - (rowY + 2)) / GLYPH_SCALE);
                setTooltipIfHovered(DocClientUtils.renderItemStack(guiGraphics, renderX, renderY, scaledMouseX, scaledMouseY, stack));
            }
            col++;
        }
        guiGraphics.pose().popPose();
    }

    @Override
    public void gatherTooltips(List<Component> list) {
        if (phaseTooltip != null) {
            list.add(phaseTooltip);
        } else if (!tooltipStack.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            Item.TooltipContext ctx = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
            list.addAll(GlyphDocTooltipHelper.getTooltipLines(tooltipStack, ctx, mc.player, TooltipFlag.Default.ADVANCED));
        }
    }

    @Override
    public void gatherTooltips(GuiGraphics stack, int mouseX, int mouseY, List<Component> tooltip) {
        if (phaseTooltip != null) {
            tooltip.add(phaseTooltip);
        } else if (!tooltipStack.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            Item.TooltipContext ctx = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
            tooltip.addAll(GlyphDocTooltipHelper.getTooltipLines(tooltipStack, ctx, mc.player, TooltipFlag.Default.ADVANCED));
        }
    }
}
