package com.github.ars_zero.common.spell;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WrappedSpellResolver extends SpellResolver {
    private final UUID castId;
    private final CastPhase phase;
    private final boolean isRootResolver;
    private static final Map<SpellResolver, UUID> resolverCastRegistry = Collections.synchronizedMap(new ConcurrentHashMap<>());
    
    public WrappedSpellResolver(SpellResolver original, UUID castId, CastPhase phase, boolean isRootResolver) {
        super(original.spellContext);
        this.castId = castId;
        this.phase = phase;
        this.isRootResolver = isRootResolver;
        
        // Copy all fields from original
        this.castType = original.castType;
        this.spell = original.spell;
        this.silent = original.silent;
        this.hitResult = original.hitResult;
        this.previousResolver = original.previousResolver;
        
        // Register this resolver
        resolverCastRegistry.put(this, castId);
    }
    
    public UUID getCastId() {
        return castId;
    }
    
    public CastPhase getPhase() {
        return phase;
    }
    
    public boolean isRootResolver() {
        return isRootResolver;
    }
    
    @Override
    public SpellResolver getNewResolver(SpellContext context) {
        // Create a new wrapped resolver for child resolvers
        SpellResolver newResolver = super.getNewResolver(context);
        return new WrappedSpellResolver(newResolver, castId, phase, false);
    }
    
    @Override
    public void resume(Level world) {
        if (world.isClientSide) {
            return;
        }
        
        // Call original resolution
        super.resume(world);
    }
    
    public static UUID getCastId(SpellResolver resolver) {
        return resolverCastRegistry.get(resolver);
    }
    
    public static void cleanup() {
        resolverCastRegistry.clear();
    }
}
