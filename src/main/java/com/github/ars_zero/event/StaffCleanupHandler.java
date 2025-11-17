package com.github.ars_zero.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "ars_zero")
public class StaffCleanupHandler {
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        
        MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.getCastContext(player, MultiPhaseCastContext.CastSource.ITEM);
        if (context == null || !context.isCasting) {
            return;
        }
        
        ItemStack heldItem = player.getMainHandItem();
        boolean isHoldingStaff = heldItem.getItem() instanceof AbstractSpellStaff;
        boolean isUsingItem = player.isUsingItem();
        
        if (!isHoldingStaff || !isUsingItem) {
            AnchorEffect.restoreEntityPhysics(context);
            AbstractMultiPhaseCastDevice.clearContext(player, MultiPhaseCastContext.CastSource.ITEM);
        }
    }
}

