package com.github.ars_zero.common.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.List;
import java.util.function.Consumer;

/**
 * Non-abstract configurable static spell staff. Create instances via {@link StaticStaffConfig}.
 */
public final class StaticStaff extends AbstractStaticSpellStaff {

    private final StaticStaffConfig config;

    public StaticStaff(StaticStaffConfig config) {
        super();
        this.config = config;
    }

    @Override
    protected String getSpellName() {
        return config.spellName();
    }

    @Override
    protected String[][] getPresetSpellIds() {
        return new String[][]{ config.beginSpellIds(), config.tickSpellIds(), config.endSpellIds() };
    }

    @Override
    protected int getPresetSlotTickDelay() {
        return config.tickDelay();
    }

    @Override
    public int getDiscountPercent() {
        return config.discountPercent();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (config.tooltipKey() != null && !config.tooltipKey().isEmpty()) {
            tooltip.add(Component.translatable(config.tooltipKey()));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        var supplier = config.rendererSupplier();
        if (supplier == null) {
            super.createGeoRenderer(consumer);
            return;
        }
        BlockEntityWithoutLevelRenderer renderer = supplier.get();
        consumer.accept(new GeoRenderProvider() {
            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return renderer;
            }
        });
    }
}
