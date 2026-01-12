package com.github.ars_zero.client.gui;

import com.github.ars_zero.common.spell.SpellPhase;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

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

    public int getContiguousGlyphCount(SpellPhase phase) {
        List<AbstractSpellPart> list = getPhaseList(phase);
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == null) {
                break;
            }
            count++;
        }
        return count;
    }

    public boolean canInsertGlyph(SpellPhase phase) {
        return getContiguousGlyphCount(phase) < getPhaseList(phase).size();
    }

    public boolean trySetGlyph(SpellPhase phase, int slot, @Nullable AbstractSpellPart part) {
        List<AbstractSpellPart> list = getPhaseList(phase);
        if (slot < 0 || slot >= list.size()) {
            return false;
        }
        int count = getContiguousGlyphCount(phase);
        if (part != null && slot > count) {
            return false;
        }
        list.set(slot, part);
        if (part == null) {
            for (int i = slot; i < list.size(); i++) {
                if (list.get(i) != null) {
                    break;
                }
                if (i > slot) {
                    list.set(i, null);
                }
            }
        }
        return true;
    }

    public boolean tryInsertGlyph(SpellPhase phase, int slot, AbstractSpellPart part) {
        List<AbstractSpellPart> list = getPhaseList(phase);
        int count = getContiguousGlyphCount(phase);
        if (count >= list.size()) {
            return false;
        }
        if (slot < 0 || slot > count) {
            return false;
        }
        for (int i = count; i > slot; i--) {
            list.set(i, list.get(i - 1));
        }
        list.set(slot, part);
        return true;
    }

    @Nullable
    public AbstractSpellPart removeGlyphAndShiftLeft(SpellPhase phase, int slot) {
        List<AbstractSpellPart> list = getPhaseList(phase);
        int count = getContiguousGlyphCount(phase);
        if (slot < 0 || slot >= count) {
            return null;
        }
        AbstractSpellPart removed = list.get(slot);
        for (int i = slot; i < count - 1; i++) {
            list.set(i, list.get(i + 1));
        }
        list.set(count - 1, null);
        return removed;
    }
}
