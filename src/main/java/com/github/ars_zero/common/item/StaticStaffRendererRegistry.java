package com.github.ars_zero.common.item;

import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.BiFunction;

/**
 * Set by the client so that StaticStaff can resolve rendererType to a GeoRenderProvider
 * without common code depending on client renderer classes.
 */
public final class StaticStaffRendererRegistry {

    /**
     * (rendererType, staff) -> GeoRenderProvider. Set from client init.
     */
    public static BiFunction<String, StaticStaff, GeoRenderProvider> RENDERER_FACTORY;

    private StaticStaffRendererRegistry() {}
}
