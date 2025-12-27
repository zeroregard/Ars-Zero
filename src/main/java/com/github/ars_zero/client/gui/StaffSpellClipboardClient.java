package com.github.ars_zero.client.gui;

import com.github.ars_zero.common.spell.StaffSpellClipboard;

import java.util.Optional;

public final class StaffSpellClipboardClient {

    private static StaffSpellClipboard clipboard;

    private StaffSpellClipboardClient() {
    }

    public static void set(StaffSpellClipboard value) {
        clipboard = value;
    }

    public static Optional<StaffSpellClipboard> get() {
        return Optional.ofNullable(clipboard);
    }
}


