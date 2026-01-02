package com.github.ars_zero.client.gui;

import com.github.ars_zero.common.spell.SpellPhase;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import java.util.ArrayList;
import java.util.List;

public class SpellPhaseSlots {
    public final List<AbstractSpellPart> begin;
    public final List<AbstractSpellPart> tick;
    public final List<AbstractSpellPart> end;

    public SpellPhaseSlots(int slotsPerPhase) {
        this.begin = new ArrayList<>();
        this.tick = new ArrayList<>();
        this.end = new ArrayList<>();

        for (int i = 0; i < slotsPerPhase; i++) {
            begin.add(null);
            tick.add(null);
            end.add(null);
        }
    }

    public List<AbstractSpellPart> getPhaseList(SpellPhase phase) {
        return switch (phase) {
            case BEGIN -> begin;
            case TICK -> tick;
            case END -> end;
        };
    }
}
