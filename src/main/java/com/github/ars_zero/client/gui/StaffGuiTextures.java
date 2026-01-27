package com.github.ars_zero.client.gui;

import com.github.ars_zero.ArsZero;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public class StaffGuiTextures {

    public static ResourceLocation getBackgroundTexture(DyeColor color) {
        String colorName = color.getName();
        return ArsZero.prefix("textures/gui/staff/background_" + colorName + ".png");
    }

    public static final ResourceLocation ICON_SLOT_SELECTED = ArsZero
            .prefix("textures/gui/abstract/icon_slot_selected.png");

    public static final ResourceLocation ICON_START = ArsZero.prefix("textures/gui/abstract/icon_start.png");
    public static final ResourceLocation ICON_START_SELECTED = ArsZero
            .prefix("textures/gui/abstract/icon_start_selected.png");

    public static final ResourceLocation ICON_TICK = ArsZero.prefix("textures/gui/abstract/icon_tick.png");
    public static final ResourceLocation ICON_TICK_SELECTED = ArsZero
            .prefix("textures/gui/abstract/icon_tick_selected.png");

    public static final ResourceLocation ICON_END = ArsZero.prefix("textures/gui/abstract/icon_end.png");
    public static final ResourceLocation ICON_END_SELECTED = ArsZero
            .prefix("textures/gui/abstract/icon_end_selected.png");

    public static final ResourceLocation ARROW_LEFT = ArsZero.prefix("textures/gui/abstract/arrow_left.png");
    public static final ResourceLocation ARROW_LEFT_HOVER = ArsZero
            .prefix("textures/gui/abstract/arrow_left_hover.png");

    public static final ResourceLocation ARROW_RIGHT = ArsZero.prefix("textures/gui/abstract/arrow_right.png");
    public static final ResourceLocation ARROW_RIGHT_HOVER = ArsZero
            .prefix("textures/gui/abstract/arrow_right_hover.png");

    public static final ResourceLocation SPELL_PHASE_ROW = ArsZero.prefix("textures/gui/abstract/spell_phase_row.png");
    public static final ResourceLocation SPELL_PHASE_ROW_SELECTED = ArsZero
            .prefix("textures/gui/abstract/spell_phase_row_selected.png");

    public static final ResourceLocation GLYPH_CATEGORY_FORM = ArsZero
            .prefix("textures/gui/abstract/icon_glyph_category_form.png");
    public static final ResourceLocation GLYPH_CATEGORY_AUGMENT = ArsZero
            .prefix("textures/gui/abstract/icon_glyph_category_augment.png");
    public static final ResourceLocation GLYPH_CATEGORY_EFFECT = ArsZero
            .prefix("textures/gui/abstract/icon_glyph_category_effect.png");
    public static final ResourceLocation GLYPH_CATEGORY_FILTER = ResourceLocation.fromNamespaceAndPath("ars_nouveau",
            "textures/gui/doc_spellcrafting_icon_filter.png");
    public static final ResourceLocation GLYPH_CATEGORY_PROPAGATE_FILTER = ResourceLocation.fromNamespaceAndPath(
            "ars_nouveau",
            "textures/gui/documentation/doc_spellcrafting_icon_subform.png");
    public static final ResourceLocation STYLE_ICON = ArsZero.prefix("textures/gui/abstract/icon_style.png");
    public static final ResourceLocation CASTING_STYLE_ICON = ArsZero.prefix("textures/gui/abstract/icon_casting.png");
    public static final ResourceLocation ICON_DISCORD = ArsZero.prefix("textures/gui/abstract/icon_discord.png");
    public static final ResourceLocation ICON_FAMILIARS = ArsZero.prefix("textures/gui/abstract/icon_familiars.png");
    public static final ResourceLocation ICON_DOCS = ArsZero.prefix("textures/gui/abstract/icon_docs.png");
    public static final ResourceLocation ICON_AFFINITY = ArsZero.prefix("textures/gui/abstract/icon_affinity.png");
}
