package com.github.ars_zero.common.spell;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MultiPhaseCastContextRegistry {
    private static final Map<UUID, MultiPhaseCastContext> contexts = new ConcurrentHashMap<>();
    
    public static MultiPhaseCastContext get(UUID castId) {
        return contexts.get(castId);
    }
    
    public static void register(MultiPhaseCastContext context) {
        contexts.put(context.castId, context);
    }
    
    public static void remove(UUID castId) {
        contexts.remove(castId);
    }
    
    public static void clear() {
        contexts.clear();
    }
}
