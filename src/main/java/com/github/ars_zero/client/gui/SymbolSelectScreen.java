package com.github.ars_zero.client.gui;

import com.github.ars_zero.common.casting.CastingStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SymbolSelectScreen extends Screen {

    private final Screen parent;
    private final CastingStyle style;
    private final List<String> options;
    private final Function<String, Component> formatter;
    private final BiConsumer<CastingStyle, String> onSelect;
    private final int panelWidth = 220;
    private final int panelHeight = 140;
    private final int cols = 2;
    private final int rowH = 18;
    private final int pad = 8;

    public SymbolSelectScreen(Screen parent, CastingStyle style, List<String> options,
            Function<String, Component> formatter, BiConsumer<CastingStyle, String> onSelect) {
        super(Component.translatable("ars_zero.gui.casting_style.symbol"));
        this.parent = parent;
        this.style = style;
        this.options = new ArrayList<>(options);
        this.formatter = formatter;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        int left = cx - panelWidth / 2;
        int top = cy - panelHeight / 2;
        int btnW = (panelWidth - pad * (cols + 1)) / cols;
        int i = 0;
        for (String opt : options) {
            int col = i % cols;
            int row = i / cols;
            int x = left + pad + col * (btnW + pad);
            int y = top + pad + 28 + row * rowH;
            Component label = formatter.apply(opt);
            Button btn = Button.builder(label, b -> choose(opt))
                .bounds(x, y, btnW, rowH - 2)
                .build();
            addRenderableWidget(btn);
            i++;
        }
    }

    private void choose(String value) {
        onSelect.accept(style, value);
        if (parent instanceof CastingStyleScreen cs) {
            cs.refreshWidgets();
        }
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x80000000);
        int cx = width / 2;
        int cy = height / 2;
        int left = cx - panelWidth / 2;
        int top = cy - panelHeight / 2;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFF1A1A1A);
        graphics.fill(left + 1, top + 1, left + panelWidth - 1, top + panelHeight - 1, 0xFF2D2D2D);
        graphics.drawString(font, getTitle(), left + pad, top + pad, 0xFFFFFF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx = width / 2;
        int cy = height / 2;
        int left = cx - panelWidth / 2;
        int top = cy - panelHeight / 2;
        if (mouseX < left || mouseX > left + panelWidth || mouseY < top || mouseY > top + panelHeight) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
