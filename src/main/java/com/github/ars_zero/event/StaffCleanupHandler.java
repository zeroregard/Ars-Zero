package com.github.ars_zero.event;

import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.IMultiPhaseCaster;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
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
        
        ItemStack heldItem = player.getMainHandItem();
        IMultiPhaseCaster caster = AbstractMultiPhaseCastDevice.asMultiPhaseCaster(player, heldItem);
        if (caster == null) {
            return;
        }
        
        MultiPhaseCastContext context = caster.getCastContext();
        if (context == null || !context.isCasting) {
            return;
        }
        
        boolean isHoldingStaff = heldItem.getItem() instanceof AbstractSpellStaff;
        boolean isUsingItem = player.isUsingItem();
        
        if (!isHoldingStaff || !isUsingItem) {
            AnchorEffect.restoreEntityPhysics(context);
            AbstractMultiPhaseCastDevice.clearContext(heldItem);
        }
    }
}

