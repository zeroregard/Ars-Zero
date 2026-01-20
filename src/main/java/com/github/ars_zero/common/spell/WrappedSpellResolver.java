package com.github.ars_zero.common.spell;

import com.github.ars_zero.common.spell.SpellPhase;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import net.minecraft.world.level.Level;

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
        
        if (context != null && context.currentPhase == SpellPhase.TICK && context.beginResults.isEmpty()) {
            return SpellPhase.BEGIN;
        }
        
        return null;
    }
}
