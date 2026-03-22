package com.github.ars_zero.common.item;

import com.github.ars_zero.client.renderer.item.CreativeFilialItemRenderer;
import com.github.ars_zero.client.renderer.model.StaticFilialGeoModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.Consumer;

public class CreativeFilialItem extends FilialItem {

    public CreativeFilialItem() {
        super("creative", null, null);
        emissive();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final BlockEntityWithoutLevelRenderer renderer =
                    new CreativeFilialItemRenderer(new StaticFilialGeoModel());

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return renderer;
            }
        });
    }
}
