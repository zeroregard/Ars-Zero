package com.github.ars_zero.common.item;

import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.Function;

/**
 * Set by the client so that StaticStaff can resolve its GeoRenderProvider
 * without common code depending on client renderer classes.
 */
public final class StaticStaffRendererRegistry {

    /** staff -> GeoRenderProvider. Set from client init. */
    public static Function<StaticStaff, GeoRenderProvider> RENDERER_FACTORY;

    private StaticStaffRendererRegistry() {}
}
