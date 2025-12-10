package com.github.ars_zero.common.spell;

import com.github.ars_zero.ArsZero;
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
        
        ArsZero.LOGGER.debug("[WrappedSpellResolver] Created - playerId: {}, phase: {}, isRoot: {}, caster type: {}", 
            playerId, phase, isRootResolver, original.spellContext.getCaster() != null ? original.spellContext.getCaster().getClass().getSimpleName() : "null");
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
}
