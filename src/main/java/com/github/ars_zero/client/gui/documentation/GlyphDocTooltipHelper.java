package com.github.ars_zero.client.gui.documentation;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.common.capability.IPlayerCap;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import com.hollingsworth.arsnouveau.setup.config.Config;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

public final class GlyphDocTooltipHelper {

    private GlyphDocTooltipHelper() {
    }

    public static List<Component> getTooltipLines(ItemStack stack, Item.TooltipContext context, Player player, TooltipFlag fallbackFlag) {
        if (stack.isEmpty()) {
            return List.of();
        }
        if (stack.getItem() instanceof Glyph glyph) {
            return buildFullGlyphTooltip(glyph, stack, player);
        }
        if (player != null && context != Item.TooltipContext.EMPTY) {
            return stack.getTooltipLines(context, player, fallbackFlag);
        }
        return stack.getTooltipLines(context, null, fallbackFlag);
    }

    private static List<Component> buildFullGlyphTooltip(Glyph glyph, ItemStack stack, Player player) {
        AbstractSpellPart spellPart = glyph.spellPart;
        List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName());
        if (spellPart == null) {
            return lines;
        }
        if (!Config.isGlyphEnabled(spellPart.getRegistryName())) {
            lines.add(Component.translatable("tooltip.ars_nouveau.glyph_disabled"));
            return lines;
        }
        lines.add(Component.translatable("tooltip.ars_nouveau.glyph_level", spellPart.getConfigTier().value)
                .setStyle(Style.EMPTY.withColor(ChatFormatting.BLUE)));
        if (!spellPart.spellSchools.isEmpty()) {
            lines.add(Component.translatable("ars_nouveau.schools"));
            for (SpellSchool s : spellPart.spellSchools) {
                lines.add(s.getTextComponent());
            }
        }
        if (player != null) {
            IPlayerCap playerDataCap = CapabilityRegistry.getPlayerDataCap(player);
            if (playerDataCap != null) {
                if (playerDataCap.knowsGlyph(spellPart) || GlyphRegistry.getDefaultStartingSpells().contains(spellPart)) {
                    lines.add(Component.translatable("tooltip.ars_nouveau.glyph_known").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN)));
                } else {
                    lines.add(Component.translatable("tooltip.ars_nouveau.glyph_unknown").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
                }
            }
        }
        lines.add(spellPart.getBookDescLang());
        return lines;
    }

    /**
     * Tooltip lines for a spell part (e.g. in static staff GUI), matching Staff/Book: "Hold shift for more info"
     * when shift is up; full tier, schools, description when shift is down.
     */
    public static List<Component> getTooltipLinesForSpellPart(AbstractSpellPart spellPart, Player player, boolean shiftDown) {
        List<Component> lines = new ArrayList<>();
        if (spellPart == null) {
            return lines;
        }
        if (!GlyphRegistry.getSpellpartMap().containsKey(spellPart.getRegistryName())) {
            return lines;
        }
        lines.add(Component.translatable(spellPart.getLocalizationKey()));
        if (shiftDown) {
            lines.add(Component.translatable("tooltip.ars_nouveau.glyph_level", spellPart.getConfigTier().value)
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.BLUE)));
            lines.add(Component.translatable("ars_nouveau.schools"));
            for (SpellSchool s : spellPart.spellSchools) {
                lines.add(s.getTextComponent());
            }
            if (player != null) {
                IPlayerCap playerDataCap = CapabilityRegistry.getPlayerDataCap(player);
                if (playerDataCap != null) {
                    if (playerDataCap.knowsGlyph(spellPart) || GlyphRegistry.getDefaultStartingSpells().contains(spellPart)) {
                        lines.add(Component.translatable("tooltip.ars_nouveau.glyph_known").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN)));
                    } else {
                        lines.add(Component.translatable("tooltip.ars_nouveau.glyph_unknown").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
                    }
                }
            }
            lines.add(spellPart.getBookDescLang());
        } else {
            if (Minecraft.getInstance().options != null && Minecraft.getInstance().options.keyShift != null) {
                lines.add(Component.translatable("tooltip.ars_nouveau.hold_shift", Minecraft.getInstance().options.keyShift.getKey().getDisplayName()));
            }
            String modName = ModList.get()
                    .getModContainerById(spellPart.getRegistryName().getNamespace())
                    .map(c -> c.getModInfo().getDisplayName())
                    .orElse(spellPart.getRegistryName().getNamespace());
            lines.add(Component.literal(modName).withStyle(ChatFormatting.BLUE));
        }
        return lines;
    }
}
