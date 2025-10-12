package com.github.ars_zero.common.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StaffCastContext {
    public final UUID castId;
    public final UUID playerId;
    public final CastPhase phase;
    public volatile boolean beginFinished = false;
    public final List<SpellResult> beginResults = new ArrayList<>();
    public final List<SpellResult> tickResults = new ArrayList<>();
    public final List<SpellResult> endResults = new ArrayList<>();
    public long createdAt = System.currentTimeMillis();
    
    public StaffCastContext(UUID castId, UUID playerId, CastPhase phase) {
        this.castId = castId;
        this.playerId = playerId;
        this.phase = phase;
    }
    
    public boolean isExpired(long maxLifetimeMs) {
        return System.currentTimeMillis() - createdAt > maxLifetimeMs;
    }
}
