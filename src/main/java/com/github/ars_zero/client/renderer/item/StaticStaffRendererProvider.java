package com.github.ars_zero.client.renderer.item;

import com.github.ars_zero.common.item.StaticStaff;
import com.github.ars_zero.common.item.StaticStaffConfig;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.Set;

/**
 * Client-only: creates the appropriate item renderer for a StaticStaff based on config.rendererType().
 */
public final class StaticStaffRendererProvider {

    private StaticStaffRendererProvider() {}

    public static GeoRenderProvider create(StaticStaff staff) {
        StaticStaffConfig config = staff.getConfig();
        if (config.visualTier() != null) {
            Set<String> hiddenBones = switch (config.visualTier()) {
                case NOVICE   -> Set.of("tier2", "tier3");
                case MAGE     -> Set.of("tier3");
                case ARCHMAGE -> Set.of();
            };
            BlockEntityWithoutLevelRenderer r = new SpellStaffStyleRenderer(hiddenBones);
            return new GeoRenderProvider() {
                @Override
                public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                    return r;
                }
            };
        }
        BlockEntityWithoutLevelRenderer r = new SpellStaffStyleRenderer(Set.of("tier3"));
        return new GeoRenderProvider() {
            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return r;
            }
        };
    }
}
