package com.github.ars_zero.common.item;

import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public interface IMultiphaseDevice {
    
    MultiPhaseCastContext getOrCreateContext(Player player, MultiPhaseCastContext.CastSource source);
    
    MultiPhaseCastContext getCastContext(Player player, MultiPhaseCastContext.CastSource source);
    
    MultiPhaseCastContext findContextByStack(Player player, ItemStack stack);
    
    void clearContext(Player player, MultiPhaseCastContext.CastSource source);
    
    default void updateCastContextPhase(MultiPhaseCastContext context, SpellPhase phase) {
        if (context == null) {
            return;
        }
        context.currentPhase = phase;
        if (phase == SpellPhase.TICK) {
            context.tickCount++;
            context.sequenceTick++;
        }
    }
    
    default void initializeCastContext(MultiPhaseCastContext context, UUID playerId, MultiPhaseCastContext.CastSource source) {
        if (context == null) {
            return;
        }
        context.currentPhase = SpellPhase.BEGIN;
        context.isCasting = true;
        context.tickCount = 0;
        context.sequenceTick = 0;
        context.outOfMana = false;
        context.createdAt = System.currentTimeMillis();
        context.beginResults.clear();
        context.tickResults.clear();
        context.endResults.clear();
        context.source = source;
    }
}
