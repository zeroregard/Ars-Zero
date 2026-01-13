package com.github.ars_zero.common.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class GeometryPlayerPreferences {

    private static final String TAG_KEY = "ars_zero_geometry_prefs";
    private static final String SIZE_KEY = "geometry_size";
    private static final String DEPTH_KEY = "geometry_depth";

    private static final int DEFAULT_SIZE = 3;
    private static final int DEFAULT_DEPTH = 1;

    private GeometryPlayerPreferences() {
    }

    public static int getPreferredSize(Player player) {
        CompoundTag tag = player.getPersistentData().getCompound(TAG_KEY);
        return tag.contains(SIZE_KEY) ? tag.getInt(SIZE_KEY) : DEFAULT_SIZE;
    }

    public static int getPreferredDepth(Player player) {
        CompoundTag tag = player.getPersistentData().getCompound(TAG_KEY);
        return tag.contains(DEPTH_KEY) ? tag.getInt(DEPTH_KEY) : DEFAULT_DEPTH;
    }

    public static void setPreferredSize(Player player, int size) {
        CompoundTag tag = player.getPersistentData().getCompound(TAG_KEY);
        tag.putInt(SIZE_KEY, size);
        player.getPersistentData().put(TAG_KEY, tag);
    }

    public static void setPreferredDepth(Player player, int depth) {
        CompoundTag tag = player.getPersistentData().getCompound(TAG_KEY);
        tag.putInt(DEPTH_KEY, depth);
        player.getPersistentData().put(TAG_KEY, tag);
    }
}

