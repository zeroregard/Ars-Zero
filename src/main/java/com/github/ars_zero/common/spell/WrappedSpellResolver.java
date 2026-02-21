package com.github.ars_zero.common.spell;

import com.github.ars_zero.common.glyph.TemporalContextForm;
import com.hollingsworth.arsnouveau.api.spell.CastResolveType;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.UUID;

public class WrappedSpellResolver extends SpellResolver {
    private final UUID playerId;
    private final SpellPhase phase;
    private final boolean isRootResolver;
    
    public WrappedSpellResolver(SpellResolver original, UUID playerId, SpellPhase phase, boolean isRootResolver) {
        super(original.spellContext);
        this.playerId = playerId;
        this.phase = phase;
        this.isRootResolver = isRootResolver;
        
        this.castType = original.castType;
        this.spell = original.spell;
        this.silent = original.silent;
        this.hitResult = original.hitResult;
        this.previousResolver = original.previousResolver;
        
        if (spellContext != null && spellContext.tag != null) {
            spellContext.tag.putString("ars_zero:spell_phase", phase.name());
            if (playerId != null) {
                spellContext.tag.putUUID("ars_zero:player_id", playerId);
            }
        }
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public SpellPhase getPhase() {
        return phase;
    }
    
    public boolean isRootResolver() {
        return isRootResolver;
    }
    
    @Override
    public SpellResolver getNewResolver(SpellContext context) {
        SpellResolver newResolver = super.getNewResolver(context);
        return new WrappedSpellResolver(newResolver, playerId, phase, false);
    }
    
    @Override
    public void onResolveEffect(Level world, HitResult result) {
        if (world.isClientSide) {
            return;
        }
        this.hitResult = result;

        // When Chaining/Burst/Wall runs with empty continuation, no effect records - so record this hit
        // so later phases (Tick/End) can use all sub-resolution targets.
        if (previousResolver != null && spell != null && spell.isEmpty() && world instanceof ServerLevel serverLevel) {
            recordSubResolutionHit(serverLevel, result);
        }

        // When entering via onResolveEffect (Chaining, Burst, Wall, etc.), the form is never run.
        // Run TemporalContextForm here so it works in sub-resolutions.
        var castMethod = spell != null ? spell.getCastMethod() : null;
        if (castMethod instanceof TemporalContextForm temporalForm && !RUNNING_TEMPORAL_FORM.get()) {
            RUNNING_TEMPORAL_FORM.set(true);
            try {
                CastResolveType resolveType = temporalForm.resolve(world, spellContext, this);
                if (resolveType == CastResolveType.SUCCESS) {
                    return;
                }
            } finally {
                RUNNING_TEMPORAL_FORM.set(false);
            }
        }

        super.onResolveEffect(world, result);
    }

    private static final ThreadLocal<Boolean> RUNNING_TEMPORAL_FORM = ThreadLocal.withInitial(() -> false);

    private void recordSubResolutionHit(ServerLevel serverLevel, HitResult result) {
        Player player = playerId != null ? serverLevel.getServer().getPlayerList().getPlayer(playerId) : null;
        IMultiPhaseCaster caster = IMultiPhaseCaster.from(spellContext, spellContext.getUnwrappedCaster());
        if (caster == null && player != null) {
            caster = IMultiPhaseCaster.from(spellContext.getCaster(), player, spellContext.getCasterTool());
        }
        if (caster == null) {
            return;
        }
        MultiPhaseCastContext context = caster.getCastContext();
        if (context == null) {
            return;
        }
        var spellResult = SpellResult.fromHitResultWithCaster(result, SpellEffectType.RESOLVED, spellContext.getCaster());
        switch (phase) {
            case BEGIN -> context.beginResults.add(spellResult);
            case TICK -> context.tickResults.add(spellResult);
            case END -> context.endResults.add(spellResult);
            default -> { }
        }
    }

    @Override
    public void resume(Level world) {
        if (world.isClientSide) {
            return;
        }
        
        super.resume(world);
    }
    
    public static SpellPhase extractPhase(SpellResolver resolver, MultiPhaseCastContext context) {
        if (resolver instanceof WrappedSpellResolver wrapped) {
            return wrapped.getPhase();
        }
        
        SpellResolver current = resolver;
        while (current != null) {
            if (current instanceof WrappedSpellResolver wrapped) {
                return wrapped.getPhase();
            }
            if (current.spellContext != null && current.spellContext.tag != null 
                    && current.spellContext.tag.contains("ars_zero:spell_phase")) {
                try {
                    return SpellPhase.valueOf(current.spellContext.tag.getString("ars_zero:spell_phase"));
                } catch (IllegalArgumentException ignored) {
                    break;
                }
            }
            current = current.previousResolver;
        }
        
        if (context != null) {
            if (context.currentPhase == SpellPhase.TICK && context.beginResults.isEmpty() && !context.beginFinished) {
                return SpellPhase.BEGIN;
            }
            if (context.currentPhase == SpellPhase.BEGIN && !context.beginFinished) {
                return SpellPhase.BEGIN;
            }
            if (context.currentPhase == SpellPhase.END) {
                return SpellPhase.END;
            }
            return context.currentPhase;
        }
        
        return null;
    }
}
