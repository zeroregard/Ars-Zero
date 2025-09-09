package com.github.ars_noita.common.spell;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StaffContextRegistry {
    private static final Map<UUID, StaffCastContext> contexts = new ConcurrentHashMap<>();
    private static final long MAX_LIFETIME_MS = 30000; // 30 seconds
    
    public static void register(StaffCastContext context) {
        contexts.put(context.castId, context);
    }
    
    public static StaffCastContext get(UUID castId) {
        return contexts.get(castId);
    }
    
    public static void remove(UUID castId) {
        contexts.remove(castId);
    }
    
    public static void cleanup() {
        contexts.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(MAX_LIFETIME_MS)
        );
    }
    
    public static void clear() {
        contexts.clear();
    }
}
