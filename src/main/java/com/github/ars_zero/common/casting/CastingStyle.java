package com.github.ars_zero.common.casting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

import java.util.HashSet;
import java.util.Set;

public class CastingStyle {
    private static final Set<String> DEFAULT_OUTLINES = Set.of("alphabet", "circle_big", "circle_small");
    public enum Placement {
        FEET,
        NEAR
    }

    private boolean enabled = false;
    private Set<String> activeBones = new HashSet<>(DEFAULT_OUTLINES);
    private int color = 0xFFFFFF;
    private int animateInTicks = 6;
    private float speed = 1.0f;
    private Placement placement = Placement.FEET;
    private boolean symbolAuto = true;
    private String selectedSymbolBone = null;

    public CastingStyle() {
    }

    public CastingStyle(boolean enabled, Set<String> activeBones, int color, int animateInTicks, float speed, Placement placement, boolean symbolAuto, String selectedSymbolBone) {
        this.enabled = enabled;
        this.activeBones = new HashSet<>(activeBones);
        this.color = color;
        this.animateInTicks = animateInTicks;
        this.speed = speed;
        this.placement = placement;
        this.symbolAuto = symbolAuto;
        this.selectedSymbolBone = selectedSymbolBone;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<String> getActiveBones() {
        return activeBones;
    }

    public void setActiveBones(Set<String> activeBones) {
        this.activeBones = new HashSet<>(activeBones);
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getAnimateInTicks() {
        return animateInTicks;
    }

    public void setAnimateInTicks(int animateInTicks) {
        this.animateInTicks = animateInTicks;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = Mth.clamp(speed, 0.5f, 10f);
    }

    public Placement getPlacement() {
        return placement;
    }

    public void setPlacement(Placement placement) {
        this.placement = placement;
    }

    public boolean isSymbolAuto() {
        return symbolAuto;
    }

    public void setSymbolAuto(boolean symbolAuto) {
        this.symbolAuto = symbolAuto;
    }

    public String getSelectedSymbolBone() {
        return selectedSymbolBone;
    }

    public void setSelectedSymbolBone(String selectedSymbolBone) {
        this.selectedSymbolBone = selectedSymbolBone;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("enabled", enabled);
        tag.putInt("color", color);
        tag.putInt("animateInTicks", animateInTicks);
        tag.putFloat("speed", speed);
        tag.putString("placement", placement.name());
        tag.putBoolean("symbolAuto", symbolAuto);
        if (selectedSymbolBone != null && !selectedSymbolBone.isEmpty()) {
            tag.putString("selectedSymbolBone", selectedSymbolBone);
        }
        if (tag.contains("anchor")) {
            tag.remove("anchor");
        }

        ListTag bonesList = new ListTag();
        for (String bone : activeBones) {
            bonesList.add(StringTag.valueOf(bone));
        }
        tag.put("activeBones", bonesList);

        return tag;
    }

    public static CastingStyle load(CompoundTag tag) {
        CastingStyle style = new CastingStyle();
        if (tag == null) {
            return style;
        }

        style.enabled = tag.getBoolean("enabled");
        style.color = tag.contains("color") ? tag.getInt("color") : 0xFFFFFF;
        style.animateInTicks = tag.contains("animateInTicks") ? tag.getInt("animateInTicks") : 6;
        if (tag.contains("speed")) {
            style.speed = Mth.clamp(tag.getFloat("speed"), 0.5f, 10f);
        } else if (tag.contains("rotationDegreesPerSecond")) {
            float rps = tag.getFloat("rotationDegreesPerSecond");
            style.speed = Mth.clamp(0.5f + (rps / 360f) * 9.5f, 0.5f, 10f);
        } else {
            style.speed = 1.0f;
        }
        style.symbolAuto = !tag.contains("symbolAuto") || tag.getBoolean("symbolAuto");

        String placementName;
        if (tag.contains("placement")) {
            placementName = tag.getString("placement");
        } else if (tag.contains("anchor")) {
            placementName = tag.getString("anchor");
        } else {
            placementName = "FEET";
        }
        try {
            style.placement = Placement.valueOf(placementName);
        } catch (IllegalArgumentException e) {
            style.placement = Placement.FEET;
        }

        if (tag.contains("activeBones", Tag.TAG_LIST)) {
            style.activeBones.clear();
            ListTag bonesList = tag.getList("activeBones", Tag.TAG_STRING);
            Set<String> symbolBones = java.util.Set.of(
                "pentagram_big", "square_small", "triangle_small",
                "school_fire", "school_water", "school_earth", "school_air",
                "school_abjuration", "school_anima", "school_conjuration", "school_manipulation"
            );
            for (int i = 0; i < bonesList.size(); i++) {
                String bone = bonesList.getString(i);
                if (symbolBones.contains(bone)) {
                    if (style.selectedSymbolBone == null) {
                        style.selectedSymbolBone = bone;
                    }
                } else {
                    style.activeBones.add(bone);
                }
            }
        }
        if (tag.contains("selectedSymbolBone")) {
            style.selectedSymbolBone = tag.getString("selectedSymbolBone");
        }

        return style;
    }

    public CastingStyle copy() {
        return new CastingStyle(enabled, activeBones, color, animateInTicks, speed, placement, symbolAuto, selectedSymbolBone);
    }
}
