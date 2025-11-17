package com.github.ars_zero.common.spell;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MultiPhaseCastContextMap {
    private final Map<MultiPhaseCastContext.CastSource, MultiPhaseCastContext> contexts = new HashMap<>();
    private final UUID playerId;
    
    public MultiPhaseCastContextMap(UUID playerId) {
        this.playerId = playerId;
    }
    
    public MultiPhaseCastContext get(MultiPhaseCastContext.CastSource source) {
        return contexts.get(source);
    }
    
    public MultiPhaseCastContext getOrCreate(MultiPhaseCastContext.CastSource source) {
        return contexts.computeIfAbsent(source, s -> new MultiPhaseCastContext(playerId, s));
    }
    
    public void remove(MultiPhaseCastContext.CastSource source) {
        contexts.remove(source);
    }
    
    public boolean isEmpty() {
        return contexts.isEmpty();
    }
    
    public Map<MultiPhaseCastContext.CastSource, MultiPhaseCastContext> getAll() {
        return new HashMap<>(contexts);
    }
}

