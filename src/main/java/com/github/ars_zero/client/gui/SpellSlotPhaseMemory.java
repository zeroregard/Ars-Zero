package com.github.ars_zero.client.gui;

import com.github.ars_zero.common.spell.SpellPhase;
import net.minecraft.world.InteractionHand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side memory of the last edited spell phase (Begin/Tick/End) per staff spell slot.
 * When switching slots or reopening the multiphase device GUI, the phase is restored.
 * Key is (playerId, slotIndex) only so it stays consistent regardless of hand or reopen.
 */
public final class SpellSlotPhaseMemory {

    private static final Map<String, SpellPhase> PHASE_BY_KEY = new HashMap<>();

    private static String key(UUID playerId, int slotIndex) {
        return playerId + "_" + slotIndex;
    }

    public static void save(UUID playerId, InteractionHand hand, int slotIndex, SpellPhase phase) {
        if (phase != null && slotIndex >= 0 && slotIndex < 10) {
            PHASE_BY_KEY.put(key(playerId, slotIndex), phase);
        }
    }

    public static SpellPhase get(UUID playerId, InteractionHand hand, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 10) return SpellPhase.BEGIN;
        return PHASE_BY_KEY.getOrDefault(key(playerId, slotIndex), SpellPhase.BEGIN);
    }
}
