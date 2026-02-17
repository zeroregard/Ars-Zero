package com.github.ars_zero.common.item;

import com.github.ars_zero.common.casting.CastingStyle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Configuration for a {@link StaticStaff}. Use the builder to construct.
 */
public record StaticStaffConfig(
    String spellName,
    String tooltipKey,
    Supplier<BlockEntityWithoutLevelRenderer> rendererSupplier,
    String[] beginSpellIds,
    String[] tickSpellIds,
    String[] endSpellIds,
    int tickDelay,
    int discountPercent,
    @Nullable CastingStyle castingStyle,
    @Nullable ResourceLocation timelineTypeId
) {

    public static Builder builder(String spellName, String tooltipKey) {
        return new Builder(spellName, tooltipKey);
    }

    public static final class Builder {
        private final String spellName;
        private final String tooltipKey;
        private Supplier<BlockEntityWithoutLevelRenderer> rendererSupplier;
        private String[] beginSpellIds;
        private String[] tickSpellIds;
        private String[] endSpellIds;
        private int tickDelay = 1;
        private int discountPercent = 0;
        private CastingStyle castingStyle = null;
        private ResourceLocation timelineTypeId = null;

        private Builder(String spellName, String tooltipKey) {
            this.spellName = spellName;
            this.tooltipKey = tooltipKey;
        }

        public Builder renderer(Supplier<BlockEntityWithoutLevelRenderer> rendererSupplier) {
            this.rendererSupplier = rendererSupplier;
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

        /** Pass no args for an empty end phase. */
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

        public Builder castingStyle(CastingStyle castingStyle) {
            this.castingStyle = castingStyle;
            return this;
        }

        public Builder timelineType(ResourceLocation timelineTypeId) {
            this.timelineTypeId = timelineTypeId;
            return this;
        }

        public StaticStaffConfig build() {
            return new StaticStaffConfig(
                spellName,
                tooltipKey,
                rendererSupplier,
                beginSpellIds,
                tickSpellIds,
                endSpellIds != null ? endSpellIds : new String[0],
                tickDelay,
                discountPercent,
                castingStyle,
                timelineTypeId
            );
        }
    }
}
