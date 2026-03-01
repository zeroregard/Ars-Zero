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

    public static GeoRenderProvider create(String rendererType, StaticStaff staff) {
        BlockEntityWithoutLevelRenderer renderer = switch (rendererType) {
            case StaticStaffConfig.RENDERER_TELEKINESIS -> new TelekinesisStaffRenderer();
            case StaticStaffConfig.RENDERER_SPELL_STAFF_STYLE -> new SpellStaffStyleRenderer(Set.of("tier3"));
            default -> new TelekinesisStaffRenderer();
        };
        BlockEntityWithoutLevelRenderer r = renderer;
        return new GeoRenderProvider() {
            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return r;
            }
        };
    }
}
