package com.github.ars_zero.common.item;

import com.github.ars_zero.client.gui.AbstractMultiPhaseCastDeviceScreen;
import com.github.ars_zero.client.gui.SpellcastingCircletGUI;
import com.github.ars_zero.client.renderer.item.ArchmageSpellStaffRenderer;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.client.registry.ModKeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.function.Consumer;

public class SpellcastingCirclet extends AbstractMultiPhaseCastDevice implements ICurioItem {
    public SpellcastingCirclet() {
        super(SpellTier.THREE, new Properties());
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    protected AbstractMultiPhaseCastDeviceScreen createDeviceScreen(ItemStack stack, InteractionHand hand) {
        return new SpellcastingCircletGUI(stack, hand);
    }

    public void beginCurioCast(Player player, ItemStack stack) {
        beginPhase(player, stack, MultiPhaseCastContext.CastSource.CURIO);
    }
    
    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            openDeviceGUI(stack, player, hand);
        }
        return InteractionResultHolder.success(stack);
    }
    
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

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Component castKey = KeyMapping.createNameSupplier(com.github.ars_zero.client.registry.ModKeyBindings.CURIO_CAST.getName()).get();
        Component radialKey = KeyMapping.createNameSupplier(ModKeyBindings.HEAD_CURIO_HOTKEY.getName()).get();
        tooltip.add(Component.translatable("ars_zero.tooltip.circlet.cast", castKey));
        tooltip.add(Component.translatable("ars_zero.tooltip.circlet.radial", radialKey));
    }
}
