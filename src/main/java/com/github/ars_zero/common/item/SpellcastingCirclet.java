package com.github.ars_zero.common.item;

import com.github.ars_zero.client.renderer.item.ArchmageSpellStaffRenderer;
import com.github.ars_zero.common.spell.StaffCastContext;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.function.Consumer;

public class SpellcastingCirclet extends AbstractSpellStaff implements ICurioItem {
    public SpellcastingCirclet() {
        super(SpellTier.THREE);
    }
    
    public void beginCurioCast(Player player, ItemStack stack) {
        beginPhase(player, stack, StaffCastContext.CastSource.CURIO);
    }
    
    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
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
