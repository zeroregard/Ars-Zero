package com.github.ars_zero.common.spell;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class WrappedSpellResolver extends SpellResolver {
    private final UUID playerId;
    private final CastPhase phase;
    private final boolean isRootResolver;
    
    public WrappedSpellResolver(SpellResolver original, UUID playerId, CastPhase phase, boolean isRootResolver) {
        super(original.spellContext);
        this.playerId = playerId;
        this.phase = phase;
        this.isRootResolver = isRootResolver;
        
        this.castType = original.castType;
        this.spell = original.spell;
        this.silent = original.silent;
        this.hitResult = original.hitResult;
        this.previousResolver = original.previousResolver;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public CastPhase getPhase() {
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
