package com.github.ars_zero.common.item;

import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for a {@link StaticStaff}. Use the builder to construct.
 * rendererType is resolved on the client (e.g. "telekinesis", "spell_staff_style").
 * Use visualTier for spell-staff-style rendering with a specific tier bone set.
 */
public record StaticStaffConfig(
    String spellName,
    String tooltipKey,
    @Nullable String rendererType,
    String[] beginSpellIds,
    String[] tickSpellIds,
    String[] endSpellIds,
    int tickDelay,
    int discountPercent,
    boolean devOnly,
    @Nullable DyeColor defaultDyeColor,
    @Nullable VisualTier visualTier,
    @Nullable String presetFilialSchool
) {

    public enum VisualTier { NOVICE, MAGE, ARCHMAGE }

    public static final String RENDERER_TELEKINESIS = "telekinesis";
    public static final String RENDERER_SPELL_STAFF_STYLE = "spell_staff_style";

    public static Builder builder(String spellName, String tooltipKey) {
        return new Builder(spellName, tooltipKey);
    }

    public static final class Builder {
        private final String spellName;
        private final String tooltipKey;
        private String rendererType;
        private String[] beginSpellIds;
        private String[] tickSpellIds;
        private String[] endSpellIds;
        private int tickDelay = 1;
        private int discountPercent = 0;
        private boolean devOnly = false;
        private DyeColor defaultDyeColor = null;
        private VisualTier visualTier = null;
        private String presetFilialSchool = null;

        private Builder(String spellName, String tooltipKey) {
            this.spellName = spellName;
            this.tooltipKey = tooltipKey;
        }

        public Builder devOnly() {
            this.devOnly = true;
            return this;
        }

        public Builder rendererType(String rendererType) {
            this.rendererType = rendererType;
            return this;
        }

        public Builder beginSpell(String... glyphIds) {
            this.beginSpellIds = glyphIds;
            return this;
        }

        public Builder tickSpell(String... glyphIds) {
            this.tickSpellIds = glyphIds;
            return this;
        }

        public Builder endSpell(String... glyphIds) {
            this.endSpellIds = glyphIds != null ? glyphIds : new String[0];
            return this;
        }

        public Builder tickDelay(int tickDelay) {
            this.tickDelay = tickDelay;
            return this;
        }

        public Builder discountPercent(int discountPercent) {
            this.discountPercent = discountPercent;
            return this;
        }

        public Builder defaultDyeColor(DyeColor color) {
            this.defaultDyeColor = color;
            return this;
        }

        public Builder visualTier(VisualTier tier) {
            this.visualTier = tier;
            return this;
        }

        public Builder presetFilialSchool(String school) {
            this.presetFilialSchool = school;
            return this;
        }

        public StaticStaffConfig build() {
            return new StaticStaffConfig(
                spellName,
                tooltipKey,
                rendererType,
                beginSpellIds,
                tickSpellIds,
                endSpellIds != null ? endSpellIds : new String[0],
                tickDelay,
                discountPercent,
                devOnly,
                defaultDyeColor,
                visualTier,
                presetFilialSchool
            );
        }
    }
}
