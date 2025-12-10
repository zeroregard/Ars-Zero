package com.github.ars_zero.client.renderer.item;

import com.github.ars_zero.ArsZero;
import com.hollingsworth.arsnouveau.client.renderer.item.GenericItemBlockRenderer;
import com.hollingsworth.arsnouveau.common.items.AnimBlockItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import java.util.function.Supplier;

public class MultiphaseTurretItemRenderer extends GenericItemBlockRenderer {
    
    private static final MultiphaseTurretItemModel MODEL = new MultiphaseTurretItemModel();
    
    public MultiphaseTurretItemRenderer() {
        super(MODEL);
    }
    
    public static Supplier<BlockEntityWithoutLevelRenderer> getISTER() {
        return () -> new MultiphaseTurretItemRenderer();
    }
    
    public static class MultiphaseTurretItemModel extends GeoModel<AnimBlockItem> {
        
        @Override
        public ResourceLocation getModelResource(AnimBlockItem animatable) {
            return ArsZero.prefix("geo/multiphase_turret.geo.json");
        }
        
        @Override
        public ResourceLocation getTextureResource(AnimBlockItem animatable) {
            return com.hollingsworth.arsnouveau.ArsNouveau.prefix("textures/block/basic_spell_turret.png");
        }
        
        @Override
        public ResourceLocation getAnimationResource(AnimBlockItem animatable) {
            return ArsZero.prefix("animations/multi_phase_spell_turret.animation.json");
        }
    }
}

