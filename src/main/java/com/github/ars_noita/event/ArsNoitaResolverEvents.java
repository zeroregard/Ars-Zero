package com.github.ars_noita.event;

import com.github.ars_noita.common.spell.CastPhase;
import com.github.ars_noita.common.spell.SpellEffectType;
import com.github.ars_noita.common.spell.SpellResult;
import com.github.ars_noita.common.spell.StaffCastContext;
import com.github.ars_noita.common.spell.StaffContextRegistry;
import com.github.ars_noita.common.spell.WrappedSpellResolver;
import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.event.SpellResolveEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.UUID;

@EventBusSubscriber(modid = "ars_noita")
public class ArsNoitaResolverEvents {
    
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
        
        // Log all effect resolutions for debugging
        com.github.ars_noita.ArsNoita.LOGGER.debug("Effect resolved: {} with hit result: {}", event.resolveEffect.getRegistryName(), event.rayTraceResult);
        
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
                    com.github.ars_noita.ArsNoita.LOGGER.debug("Added Begin result: {} (total: {})", result.hitResult, context.beginResults.size());
                }
                case TICK -> {
                    context.tickResults.add(result);
                    com.github.ars_noita.ArsNoita.LOGGER.debug("Added Tick result: {} (total: {})", result.hitResult, context.tickResults.size());
                }
                case END -> {
                    context.endResults.add(result);
                    com.github.ars_noita.ArsNoita.LOGGER.debug("Added End result: {} (total: {})", result.hitResult, context.endResults.size());
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
