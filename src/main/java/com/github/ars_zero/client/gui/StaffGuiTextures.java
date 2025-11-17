package com.github.ars_zero.client.gui;

import com.github.ars_zero.ArsZero;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public class StaffGuiTextures {
    
    public static ResourceLocation getBackgroundTexture(DyeColor color) {
        String colorName = color.getName();
        return ArsZero.prefix("textures/gui/staffui_background_" + colorName + ".png");
    }
    
    public static final ResourceLocation ICON_SLOT_SELECTED = ArsZero.prefix("textures/gui/staffui_icon_slot_selected.png");
    
    public static final ResourceLocation ICON_START = ArsZero.prefix("textures/gui/staffui_icon_start.png");
    public static final ResourceLocation ICON_START_SELECTED = ArsZero.prefix("textures/gui/staffui_icon_start_selected.png");
    
    public static final ResourceLocation ICON_TICK = ArsZero.prefix("textures/gui/staffui_icon_tick.png");
    public static final ResourceLocation ICON_TICK_SELECTED = ArsZero.prefix("textures/gui/staffui_icon_tick_selected.png");
    
    public static final ResourceLocation ICON_END = ArsZero.prefix("textures/gui/staffui_icon_end.png");
    public static final ResourceLocation ICON_END_SELECTED = ArsZero.prefix("textures/gui/staffui_icon_end_selected.png");
    
    public static final ResourceLocation ARROW_LEFT = ArsZero.prefix("textures/gui/staffui_arrow_left.png");
    public static final ResourceLocation ARROW_LEFT_HOVER = ArsZero.prefix("textures/gui/staffui_arrow_left_hover.png");
    
    public static final ResourceLocation ARROW_RIGHT = ArsZero.prefix("textures/gui/staffui_arrow_right.png");
    public static final ResourceLocation ARROW_RIGHT_HOVER = ArsZero.prefix("textures/gui/staffui_arrow_right_hover.png");
    
    public static final ResourceLocation SPELL_PHASE_ROW = ArsZero.prefix("textures/gui/spell_phase_row.png");
    public static final ResourceLocation SPELL_PHASE_ROW_SELECTED = ArsZero.prefix("textures/gui/spell_phase_row_selected.png");
    
    public static final ResourceLocation GLYPH_CATEGORY_FORM = ArsZero.prefix("textures/gui/icon_glyph_category_form.png");
    public static final ResourceLocation GLYPH_CATEGORY_AUGMENT = ArsZero.prefix("textures/gui/icon_glyph_category_augment.png");
    public static final ResourceLocation GLYPH_CATEGORY_EFFECT = ArsZero.prefix("textures/gui/icon_glyph_category_effect.png");
    public static final ResourceLocation STYLE_ICON = ArsZero.prefix("textures/gui/icon_style.png");
}

