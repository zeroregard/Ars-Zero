package com.github.ars_noita.client.gui;

import com.github.ars_noita.ArsNoita;
import com.github.ars_noita.common.item.ArsNoitaStaff;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ArsNoitaStaffGUI extends Screen {
    
    private static final ResourceLocation GUI_TEXTURE = ArsNoita.prefix("textures/gui/staff_gui.png");
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 256;
    
    private int guiLeft;
    private int guiTop;
    
    private ArsNoitaStaff.StaffPhase currentSelectedPhase = ArsNoitaStaff.StaffPhase.BEGIN;
    
    private Button beginPhaseButton;
    private Button tickPhaseButton;
    private Button endPhaseButton;
    
    public ArsNoitaStaffGUI() {
        super(Component.translatable("gui.ars_noita.staff.title"));
    }

    @Override
    protected void init() {
        super.init();
        
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // Create phase selection buttons
        this.beginPhaseButton = Button.builder(
            Component.translatable("gui.ars_noita.staff.begin_phase"),
            button -> selectPhase(ArsNoitaStaff.StaffPhase.BEGIN)
        ).bounds(guiLeft + 10, guiTop + 200, 70, 20).build();
        
        this.tickPhaseButton = Button.builder(
            Component.translatable("gui.ars_noita.staff.tick_phase"),
            button -> selectPhase(ArsNoitaStaff.StaffPhase.TICK)
        ).bounds(guiLeft + 90, guiTop + 200, 70, 20).build();
        
        this.endPhaseButton = Button.builder(
            Component.translatable("gui.ars_noita.staff.end_phase"),
            button -> selectPhase(ArsNoitaStaff.StaffPhase.END)
        ).bounds(guiLeft + 170, guiTop + 200, 70, 20).build();
        
        this.addRenderableWidget(beginPhaseButton);
        this.addRenderableWidget(tickPhaseButton);
        this.addRenderableWidget(endPhaseButton);
        
        updateButtonStates();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        
        // Render GUI background
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        graphics.blit(GUI_TEXTURE, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        
        // Render phase sections
        renderPhaseSection(graphics, ArsNoitaStaff.StaffPhase.BEGIN, guiLeft + 10, guiTop + 30);
        renderPhaseSection(graphics, ArsNoitaStaff.StaffPhase.TICK, guiLeft + 10, guiTop + 80);
        renderPhaseSection(graphics, ArsNoitaStaff.StaffPhase.END, guiLeft + 10, guiTop + 130);
        
        // Render current phase indicator
        renderCurrentPhaseIndicator(graphics);
        
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderPhaseSection(GuiGraphics graphics, ArsNoitaStaff.StaffPhase phase, int x, int y) {
        // Render phase label
        Component phaseLabel = Component.translatable("gui.ars_noita.staff." + phase.name().toLowerCase() + "_phase");
        graphics.drawString(this.font, phaseLabel, x, y, 0xFFFFFF);
        
        // Render spell slots for this phase (placeholder)
        for (int i = 0; i < 8; i++) {
            int slotX = x + (i * 20);
            int slotY = y + 15;
            
            // Render empty slot
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF404040);
            graphics.fill(slotX + 1, slotY + 1, slotX + 15, slotY + 15, 0xFF808080);
        }
        
        // Highlight current selected phase
        if (phase == currentSelectedPhase) {
            graphics.fill(x - 2, y - 2, x + 180, y + 35, 0x80FFFF00);
        }
    }

    private void renderCurrentPhaseIndicator(GuiGraphics graphics) {
        Component indicator = Component.translatable("gui.ars_noita.staff.current_phase", 
            Component.translatable("gui.ars_noita.staff." + currentSelectedPhase.name().toLowerCase() + "_phase"));
        graphics.drawString(this.font, indicator, guiLeft + 10, guiTop + 10, 0xFFFF00);
    }

    private void selectPhase(ArsNoitaStaff.StaffPhase phase) {
        this.currentSelectedPhase = phase;
        updateButtonStates();
    }

    private void updateButtonStates() {
        // Update button states based on current selection
        beginPhaseButton.setMessage(Component.translatable("gui.ars_noita.staff.begin_phase" + 
            (currentSelectedPhase == ArsNoitaStaff.StaffPhase.BEGIN ? "_selected" : "")));
        tickPhaseButton.setMessage(Component.translatable("gui.ars_noita.staff.tick_phase" + 
            (currentSelectedPhase == ArsNoitaStaff.StaffPhase.TICK ? "_selected" : "")));
        endPhaseButton.setMessage(Component.translatable("gui.ars_noita.staff.end_phase" + 
            (currentSelectedPhase == ArsNoitaStaff.StaffPhase.END ? "_selected" : "")));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
