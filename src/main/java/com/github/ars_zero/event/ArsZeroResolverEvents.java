package com.github.ars_zero.event;

import com.github.ars_zero.common.spell.CastPhase;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.StaffCastContext;
import com.github.ars_zero.common.spell.StaffContextRegistry;
import com.github.ars_zero.common.spell.WrappedSpellResolver;
import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.event.SpellResolveEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.UUID;

@EventBusSubscriber(modid = "ars_zero")
public class ArsZeroResolverEvents {
    
    @SubscribeEvent
    public static void onEffectResolved(EffectResolveEvent.Post event) {
        if (event.world.isClientSide) {
            return;
        }
        
        UUID castId = WrappedSpellResolver.getCastId(event.resolver);
        if (castId == null) {
            return; // Not our staff cast
        }
        
        StaffCastContext context = StaffContextRegistry.get(castId);
        if (context == null) {
            return;
        }
        
        
        // Only capture Begin-phase results
        if (event.resolver instanceof WrappedSpellResolver wrapped) {
            if (wrapped.getPhase() != CastPhase.BEGIN) {
                return;
            }
        }
        
        // Create SpellResult from the hit result
        SpellResult result = SpellResult.fromHitResult(event.rayTraceResult, SpellEffectType.RESOLVED);
        
        // Add to appropriate results list based on phase
        if (event.resolver instanceof WrappedSpellResolver wrapped) {
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
    }
    
    @SubscribeEvent
    public static void onSpellResolved(SpellResolveEvent.Post event) {
        if (event.world.isClientSide) {
            return;
        }
        
        UUID castId = WrappedSpellResolver.getCastId(event.resolver);
        if (castId == null) {
            return;
        }
        
        StaffCastContext context = StaffContextRegistry.get(castId);
        if (context == null) {
            return;
        }
        
        // If this is a root resolver for Begin phase, mark it as finished
        if (event.resolver instanceof WrappedSpellResolver wrapped) {
            if (wrapped.isRootResolver() && wrapped.getPhase() == CastPhase.BEGIN) {
                context.beginFinished = true;
            }
        }
    }
}
