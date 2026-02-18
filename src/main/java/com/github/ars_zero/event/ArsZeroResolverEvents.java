package com.github.ars_zero.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.spell.IMultiPhaseCaster;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellPhase;
import com.github.ars_zero.common.spell.TemporalContextRecorder;
import com.github.ars_zero.common.spell.WrappedSpellResolver;
import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.event.SpellResolveEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.IWrappedCaster;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.TileCaster;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

@EventBusSubscriber(modid = "ars_zero")
public class ArsZeroResolverEvents {
    
    @SubscribeEvent
    public static void onEffectResolving(com.hollingsworth.arsnouveau.api.event.EffectResolveEvent.Pre event) {
        if (event.world.isClientSide) {
            return;
        }
        
        if (!(event.resolver instanceof WrappedSpellResolver wrapped)) {
            return;
        }
        
        if (wrapped.getPhase() != SpellPhase.BEGIN) {
            return;
        }
        // Block groups are created only by the Select effect; no pre-capture or implicit group creation here.
    }
    
    @SubscribeEvent
    public static void onEffectResolved(EffectResolveEvent.Post event) {
        if (event.world.isClientSide) {
            return;
        }
        if (!(event.resolver instanceof WrappedSpellResolver wrapped)) {
            return;
        }

        ServerLevel serverLevel = event.world instanceof ServerLevel level ? level : null;
        ResourceKey<Level> dimensionKey = serverLevel != null ? serverLevel.dimension() : null;

        ResolveContext resolveCtx = getResolveContext(event, wrapped, serverLevel);
        if (resolveCtx == null) {
            cleanupCapturedState(dimensionKey);
            return;
        }

        ResolveResult resolveResult = buildResultsFromEvent(event, wrapped, resolveCtx.player, serverLevel, dimensionKey);
        cleanupCapturedState(dimensionKey);

        if (resolveResult == null || resolveResult.results.isEmpty()) {
            return;
        }

        applyResults(resolveCtx.context, wrapped.getPhase(), resolveResult);
    }

    private static void cleanupCapturedState(@SuppressWarnings("unused") ResourceKey<Level> dimensionKey) {
        // No-op: block groups are created only by Select; no per-dimension state to clear.
    }

    private static ResolveContext getResolveContext(EffectResolveEvent.Post event, WrappedSpellResolver wrapped,
            ServerLevel serverLevel) {
        IMultiPhaseCaster multiPhaseCaster = IMultiPhaseCaster.from(event.resolver.spellContext, null);
        if (multiPhaseCaster != null) {
            MultiPhaseCastContext context = multiPhaseCaster.getCastContext();
            if (context == null) {
                ArsZero.LOGGER.warn("[ArsZeroResolverEvents] IMultiPhaseCaster has no cast context!");
                return null;
            }
            Player player = serverLevel != null ? serverLevel.getServer().getPlayerList().getPlayer(wrapped.getPlayerId()) : null;
            return new ResolveContext(context, player);
        }

        Player player = serverLevel != null ? serverLevel.getServer().getPlayerList().getPlayer(wrapped.getPlayerId()) : null;
        if (player == null) {
            return null;
        }
        IMultiPhaseCaster caster = IMultiPhaseCaster.from(event.resolver.spellContext, player);
        if (caster == null) {
            return null;
        }
        MultiPhaseCastContext context = caster.getCastContext();
        if (context == null) {
            return null;
        }
        return new ResolveContext(context, player);
    }

    private static ResolveResult buildResultsFromEvent(EffectResolveEvent.Post event, WrappedSpellResolver wrapped,
            Player player, ServerLevel serverLevel, ResourceKey<Level> dimensionKey) {
        List<SpellResult> recorded = TemporalContextRecorder.take(event.resolver.spellContext, event.world);
        if (recorded != null && !recorded.isEmpty()) {
            // Sub-resolutions (Chaining, Burst, Wall) each contribute; append instead of replace.
            boolean replace = event.resolver.previousResolver == null;
            return new ResolveResult(recorded, replace);
        }

        HitResult hitResult = event.rayTraceResult;
        SpellResult fromHit = SpellResult.fromHitResultWithCaster(hitResult, SpellEffectType.RESOLVED, event.resolver.spellContext.getCaster());
        return new ResolveResult(List.of(fromHit), false);
    }

    private static void applyResults(MultiPhaseCastContext context, SpellPhase phase, ResolveResult resolveResult) {
        List<SpellResult> results = resolveResult.results;
        boolean replace = resolveResult.replace;

        switch (phase) {
            case BEGIN -> {
                if (replace) {
                    context.beginResults.clear();
                    context.beginResults.addAll(results);
                } else {
                    for (SpellResult result : results) {
                        if (result == null) continue;
                        if (result.hitResult instanceof BlockHitResult && context.beginResults.stream()
                                .anyMatch(r -> r != null && r.hitResult instanceof EntityHitResult)) {
                            ArsZero.LOGGER.debug("[ArsZeroResolverEvents] BEGIN: Skipping block result, entity result already exists");
                            continue;
                        }
                        context.beginResults.add(result);
                    }
                }
            }
            case TICK -> {
                if (replace) {
                    context.tickResults.clear();
                }
                results.stream().filter(r -> r != null).forEach(context.tickResults::add);
            }
            case END -> {
                if (replace) {
                    context.endResults.clear();
                }
                results.stream().filter(r -> r != null).forEach(context.endResults::add);
            }
        }
    }

    private record ResolveContext(MultiPhaseCastContext context, Player player) {}
    private record ResolveResult(List<SpellResult> results, boolean replace) {}

    @SubscribeEvent
    public static void onSpellResolved(SpellResolveEvent.Post event) {
        if (event.world.isClientSide) {
            return;
        }
        
        if (!(event.resolver instanceof WrappedSpellResolver wrapped)) {
            return;
        }
        
        if (!wrapped.isRootResolver() || wrapped.getPhase() != SpellPhase.BEGIN) {
            return;
        }

        ServerLevel serverLevel = event.world instanceof ServerLevel level ? level : null;
        Player player = serverLevel != null ? serverLevel.getServer().getPlayerList().getPlayer(wrapped.getPlayerId()) : null;
        IMultiPhaseCaster caster = null;
        
        if (player != null) {
            caster = IMultiPhaseCaster.from(event.resolver.spellContext, player);
        } else {
            IWrappedCaster wrappedCaster = event.resolver.spellContext.getCaster();
            if (wrappedCaster != null) {
                BlockEntity tile = getBlockEntityFromCaster(wrappedCaster);
                if (tile instanceof IMultiPhaseCaster multiPhaseCaster) {
                    caster = multiPhaseCaster;
                }
            }
        }
        
        if (caster == null) {
            return;
        }

        MultiPhaseCastContext context = caster.getCastContext();
        if (context == null) {
            return;
        }

        context.beginFinished = true;
    }
    
    private static BlockEntity getBlockEntityFromCaster(IWrappedCaster wrappedCaster) {
        if (wrappedCaster instanceof TileCaster tileCaster) {
            return tileCaster.getTile();
        }
        return null;
    }
}
