package com.github.ars_zero.event;

import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.AbstractStaff;
import com.github.ars_zero.common.spell.IMultiPhaseCaster;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.MultiPhaseCastContextRegistry;
import com.github.ars_zero.common.spell.MultiPhaseCastContext.CastSource;
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
        MultiPhaseCastContext context = caster != null ? caster.getCastContext() : null;
        
        if (context != null && context.isCasting) {
            boolean isHoldingStaff = heldItem.getItem() instanceof AbstractStaff;
            boolean isUsingItem = player.isUsingItem();
            
            if (!isHoldingStaff || !isUsingItem) {
                AnchorEffect.restoreEntityPhysics(context);
                AbstractMultiPhaseCastDevice.clearContext(heldItem);
            }
            return;
        }
        
        // Orphan detection: player swapped away from staff while channeling
        for (MultiPhaseCastContext ctx : MultiPhaseCastContextRegistry.getActiveContextsForPlayer(player.getUUID())) {
            if (ctx.source != CastSource.ITEM) continue;
            
            boolean inHand = AbstractMultiPhaseCastDevice.findContextByStack(player, player.getMainHandItem()) == ctx
                    || AbstractMultiPhaseCastDevice.findContextByStack(player, player.getOffhandItem()) == ctx;
            if (!inHand && !ctx.castingStack.isEmpty() && ctx.castingStack.getItem() instanceof AbstractMultiPhaseCastDevice device) {
                AnchorEffect.restoreEntityPhysics(ctx);
                device.endPhase(player, ctx.castingStack);
            }
        }
    }
}

