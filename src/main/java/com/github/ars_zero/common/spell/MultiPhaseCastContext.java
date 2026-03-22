package com.github.ars_zero.common.spell;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MultiPhaseCastContext {
    public enum CastSource {
        ITEM,
        CURIO,
        TURRET
    }
    
    public final UUID castId;
    public final UUID playerId;
    public SpellPhase currentPhase = SpellPhase.BEGIN;
    public boolean isCasting = false;
    public int tickCount = 0;
    public int sequenceTick = 0;
    public boolean outOfMana = false;
    public CastSource source = CastSource.ITEM;
    public ItemStack castingStack = ItemStack.EMPTY;
    
    public volatile boolean beginFinished = false;
    public final List<SpellResult> beginResults = new ArrayList<>();
    public final List<SpellResult> tickResults = new ArrayList<>();
    public final List<SpellResult> endResults = new ArrayList<>();
    public long createdAt = System.currentTimeMillis();
    
    public double distanceMultiplier = 1.0;
    public int tickCooldown = 0;
    /** True after the first TICK has fired this cast; ensures first tick always fires, delay applies only to subsequent ticks. */
    public boolean hasFiredFirstTick = false;
    /** Last game tick when we executed TICK; prevents double execution when onUseTick and releaseUsing both call tickPhase in same tick. */
    public long lastExecuteGameTick = -1;

    public Entity arcaneCircleEntity = null;
    
    public MultiPhaseCastContext(UUID playerId, CastSource source) {
        this.castId = UUID.randomUUID();
        this.playerId = playerId;
        this.source = source;
    }
    
    public boolean isExpired(long maxLifetimeMs) {
        return System.currentTimeMillis() - createdAt > maxLifetimeMs;
    }
}

