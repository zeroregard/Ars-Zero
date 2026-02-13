package com.github.ars_zero.client.gui.documentation;

import com.hollingsworth.arsnouveau.api.documentation.DocClientUtils;
import com.hollingsworth.arsnouveau.api.documentation.SinglePageCtor;
import com.hollingsworth.arsnouveau.api.documentation.SinglePageWidget;
import com.hollingsworth.arsnouveau.client.gui.documentation.BaseDocScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Renders only a row of glyph item stacks (e.g. the 4 lifespan-based glyphs) with hover tooltips.
 * Used as the second page of the lifespan-based effects tome entry.
 */
public class LifespanGlyphsOnlyPage extends SinglePageWidget {

    private static final float GLYPH_SCALE = 1.0f;
    private static final int SLOT_SIZE = 16;
    private static final int PADDING = 2;

    private final List<ItemStack> stacks;

    public LifespanGlyphsOnlyPage(List<ItemStack> stacks, BaseDocScreen parent, int x, int y, int width, int height) {
        super(parent, x, y, width, height);
        this.stacks = stacks != null ? stacks : List.of();
    }

    public static SinglePageCtor create(List<ItemStack> stacks) {
        return (parent, x, y, width, height) -> new LifespanGlyphsOnlyPage(stacks, parent, x, y, width, height);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        if (stacks.isEmpty()) {
            return;
        }
        int baseX = getX();
        int baseY = getY();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(baseX, baseY, 0);
        guiGraphics.pose().scale(GLYPH_SCALE, GLYPH_SCALE, 1f);
        int col = 0;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                int renderX = col * (SLOT_SIZE + PADDING);
                int renderY = 0;
                int scaledMouseX = (int) ((mouseX - baseX) / GLYPH_SCALE);
                int scaledMouseY = (int) ((mouseY - baseY) / GLYPH_SCALE);
                setTooltipIfHovered(DocClientUtils.renderItemStack(guiGraphics, renderX, renderY, scaledMouseX, scaledMouseY, stack));
            }
            col++;
        }
        guiGraphics.pose().popPose();
    }

    public void gatherTooltips(List<Component> list) {
        if (!tooltipStack.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            Item.TooltipContext ctx = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
            list.addAll(GlyphDocTooltipHelper.getTooltipLines(tooltipStack, ctx, mc.player, TooltipFlag.Default.ADVANCED));
        }
    }

    public void gatherTooltips(GuiGraphics stack, int mouseX, int mouseY, List<Component> tooltip) {
        gatherTooltips(tooltip);
    }
}
