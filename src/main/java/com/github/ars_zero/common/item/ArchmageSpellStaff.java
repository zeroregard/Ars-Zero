package com.github.ars_zero.common.item;

import com.github.ars_zero.client.renderer.item.ArchmageSpellStaffRenderer;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.Consumer;

public class ArchmageSpellStaff extends AbstractSpellStaff {
    public ArchmageSpellStaff() {
        super(SpellTier.THREE);
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final BlockEntityWithoutLevelRenderer renderer = new ArchmageSpellStaffRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                return renderer;
            }
        });
    }
}

