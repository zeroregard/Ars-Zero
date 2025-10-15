package com.github.ars_zero.event;

import com.github.ars_zero.common.item.ArsZeroStaff;
import com.github.ars_zero.common.spell.CastPhase;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.StaffCastContext;
import com.github.ars_zero.common.spell.WrappedSpellResolver;
import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.event.SpellResolveEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "ars_zero")
public class ArsZeroResolverEvents {
    
    @SubscribeEvent
    public static void onEffectResolved(EffectResolveEvent.Post event) {
        if (event.world.isClientSide) {
            return;
        }
        
        if (!(event.resolver instanceof WrappedSpellResolver wrapped)) {
            return;
        }
        
        if (wrapped.getPhase() != CastPhase.BEGIN) {
            return;
        }
        
        Player player = ((ServerLevel) event.world).getServer().getPlayerList().getPlayer(wrapped.getPlayerId());
        if (player == null) {
            return;
        }
        
        StaffCastContext context = ArsZeroStaff.getStaffContext(player);
        if (context == null) {
            return;
        }
        
        SpellResult result = SpellResult.fromHitResult(event.rayTraceResult, SpellEffectType.RESOLVED);
        
        switch (wrapped.getPhase()) {
            case BEGIN -> {
                context.beginResults.add(result);
                com.github.ars_zero.ArsZero.LOGGER.debug("Added Begin result: {} (total: {})", result.hitResult, context.beginResults.size());
            }
            case TICK -> {
                context.tickResults.add(result);
            }
            case END -> {
                context.endResults.add(result);
            }
        }
    }
    
    @SubscribeEvent
    public static void onSpellResolved(SpellResolveEvent.Post event) {
        if (event.world.isClientSide) {
            return;
        }
        
        if (!(event.resolver instanceof WrappedSpellResolver wrapped)) {
            return;
        }
        
        if (!wrapped.isRootResolver() || wrapped.getPhase() != CastPhase.BEGIN) {
            return;
        }
        
        Player player = ((ServerLevel) event.world).getServer().getPlayerList().getPlayer(wrapped.getPlayerId());
        if (player == null) {
            return;
        }
        
        StaffCastContext context = ArsZeroStaff.getStaffContext(player);
        if (context == null) {
            return;
        }
        
        context.beginFinished = true;
    }
}
