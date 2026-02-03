package com.github.ars_zero.client.gui.documentation;

import com.hollingsworth.arsnouveau.api.documentation.DocClientUtils;
import com.hollingsworth.arsnouveau.api.documentation.SinglePageCtor;
import com.hollingsworth.arsnouveau.api.documentation.SinglePageWidget;
import com.hollingsworth.arsnouveau.client.gui.documentation.BaseDocScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ScaledGlyphStripEntry extends SinglePageWidget {

    private static final float SCALE = 0.5f;
    private static final int SLOT_SIZE_FULL = 16;
    private static final int PADDING = 2;
    private static final int SLOTS_PER_ROW = 8;

    private final List<ItemStack> stacks;

    public ScaledGlyphStripEntry(List<ItemStack> stacks, BaseDocScreen parent, int x, int y, int width, int height) {
        super(parent, x, y, width, height);
        this.stacks = stacks;
    }

    public static SinglePageCtor create(List<ItemStack> stacks) {
        return (parent, x, y, width, height) -> new ScaledGlyphStripEntry(stacks, parent, x, y, width, height);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        if (stacks == null || stacks.isEmpty()) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getX(), getY(), 0);
        guiGraphics.pose().scale(SCALE, SCALE, 1f);
        int row = 0;
        int col = 0;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                int renderX = col * (SLOT_SIZE_FULL + PADDING);
                int renderY = row * (SLOT_SIZE_FULL + PADDING);
                int scaledMouseX = (int) ((mouseX - getX()) / SCALE);
                int scaledMouseY = (int) ((mouseY - getY()) / SCALE);
                setTooltipIfHovered(DocClientUtils.renderItemStack(guiGraphics, renderX, renderY, scaledMouseX, scaledMouseY, stack));
            }
            col++;
            if (col >= SLOTS_PER_ROW) {
                col = 0;
                row++;
            }
        }
        guiGraphics.pose().popPose();
    }
}
