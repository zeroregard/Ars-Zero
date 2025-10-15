package com.github.ars_zero.event;

import com.github.ars_zero.common.glyph.TranslateEffect;
import com.github.ars_zero.common.item.ArsZeroStaff;
import com.github.ars_zero.common.spell.StaffCastContext;
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
        
        StaffCastContext context = ArsZeroStaff.getStaffContext(player);
        if (context == null || !context.isHoldingStaff) {
            return;
        }
        
        ItemStack heldItem = player.getMainHandItem();
        boolean isHoldingStaff = heldItem.getItem() instanceof ArsZeroStaff;
        boolean isUsingItem = player.isUsingItem();
        
        if (!isHoldingStaff || !isUsingItem) {
            TranslateEffect.restoreEntityPhysics(context);
            player.removeData(com.github.ars_zero.registry.ModAttachments.STAFF_CONTEXT);
            com.github.ars_zero.ArsZero.LOGGER.debug("Cleaned up staff context for {} (switched items or stopped using)", player.getName().getString());
        }
    }
}

