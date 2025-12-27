package com.github.ars_zero.common.spell;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.util.ANCodecs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

public record StaffSpellClipboard(Spell begin, Spell tick, Spell end, String name, int tickDelay) {

    private static final String CLIPBOARD_KEY = "ars_zero_staff_clipboard";
    private static final String BEGIN_KEY = "begin";
    private static final String TICK_KEY = "tick";
    private static final String END_KEY = "end";
    private static final String NAME_KEY = "name";
    private static final String DELAY_KEY = "delay";

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        putSpell(tag, BEGIN_KEY, begin);
        putSpell(tag, TICK_KEY, tick);
        putSpell(tag, END_KEY, end);
        if (name != null && !name.isBlank()) {
            tag.putString(NAME_KEY, name);
        }
        tag.putInt(DELAY_KEY, tickDelay);
        return tag;
    }

    public static Optional<StaffSpellClipboard> fromTag(CompoundTag tag) {
        if (tag == null) {
            return Optional.empty();
        }
        Spell begin = getSpell(tag, BEGIN_KEY);
        Spell tick = getSpell(tag, TICK_KEY);
        Spell end = getSpell(tag, END_KEY);
        String name = tag.contains(NAME_KEY, Tag.TAG_STRING) ? tag.getString(NAME_KEY) : "";
        int delay = tag.contains(DELAY_KEY, Tag.TAG_INT) ? tag.getInt(DELAY_KEY) : 1;
        if ((begin == null || begin.isEmpty()) && (tick == null || tick.isEmpty()) && (end == null || end.isEmpty()) && name.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new StaffSpellClipboard(
            begin == null ? new Spell() : begin,
            tick == null ? new Spell() : tick,
            end == null ? new Spell() : end,
            name,
            delay
        ));
    }

    public static Optional<StaffSpellClipboard> readFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(CLIPBOARD_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return fromTag(root.getCompound(CLIPBOARD_KEY));
    }

    public static void writeToStack(ItemStack stack, StaffSpellClipboard clipboard) {
        if (stack == null || stack.isEmpty() || clipboard == null) {
            return;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = data != null ? data.copyTag() : new CompoundTag();
        root.put(CLIPBOARD_KEY, clipboard.toTag());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static void putSpell(CompoundTag tag, String key, Spell spell) {
        if (spell == null || spell.isEmpty()) {
            tag.remove(key);
            return;
        }
        Tag data = ANCodecs.encode(Spell.CODEC.codec(), spell);
        if (data != null) {
            tag.put(key, data);
        }
    }

    private static Spell getSpell(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return new Spell();
        }
        Tag data = tag.get(key);
        Spell loaded = (Spell) ANCodecs.decode(Spell.CODEC.codec(), data);
        return loaded == null ? new Spell() : loaded;
    }
}


