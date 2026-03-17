package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT,
            ArsZero.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> EFFECT_PUSH = SOUNDS.register(
            "effect_push",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("effect_push")));

    public static final DeferredHolder<SoundEvent, SoundEvent> EFFECT_ANCHOR = SOUNDS.register(
            "effect_anchor",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("effect_anchor")));

    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHTNING_VOXEL_HIT = SOUNDS.register(
            "lightning_voxel_hit",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("lightning_voxel_hit")));

    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_CHARGE = SOUNDS.register(
            "explosion_charge",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("explosion_charge")));

    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_IDLE = SOUNDS.register(
            "explosion_idle",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("explosion_idle")));

    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_ACTIVATE = SOUNDS.register(
            "explosion_activate",
            () -> SoundEvent.createFixedRangeEvent(ArsZero.prefix("explosion_activate"), 1000.0f));

    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_PRIMING = SOUNDS.register(
            "explosion_priming",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("explosion_priming")));

    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_DISTANT = SOUNDS.register(
            "explosion_distant",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("explosion_distant")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SPLASH_FAST = SOUNDS.register(
            "splash_fast",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("splash_fast")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_MAGE_AMBIENT = SOUNDS.register(
            "undead_mage.ambient",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("undead_mage.ambient")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_MAGE_HURT = SOUNDS.register(
            "undead_mage.hurt",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("undead_mage.hurt")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_MAGE_DEATH = SOUNDS.register(
            "undead_mage.death",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("undead_mage.death")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_MAGE_STEP = SOUNDS.register(
            "undead_mage.step",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("undead_mage.step")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BONE_GOLEM_HURT = SOUNDS.register(
            "bone_golem.hurt",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("bone_golem.hurt")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BONE_GOLEM_DEATH = SOUNDS.register(
            "bone_golem.death",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("bone_golem.death")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BONE_GOLEM_STEP = SOUNDS.register(
            "bone_golem.step",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("bone_golem.step")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BONE_GOLEM_ATTACK = SOUNDS.register(
            "bone_golem.attack",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("bone_golem.attack")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BONE_GOLEM_REPAIR = SOUNDS.register(
            "bone_golem.repair",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("bone_golem.repair")));

    public static final DeferredHolder<SoundEvent, SoundEvent> NECROPOLIS_AMBIENT = SOUNDS.register(
            "ambient.necropolis",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("ambient.necropolis")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BONE_CHEST_OPEN = SOUNDS.register(
            "bone_chest.open",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("bone_chest.open")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BONE_CHEST_CLOSE = SOUNDS.register(
            "bone_chest.close",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("bone_chest.close")));

    public static final DeferredHolder<SoundEvent, SoundEvent> LICH_AMBIENT = SOUNDS.register(
            "lich.ambient",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("lich.ambient")));

    public static final DeferredHolder<SoundEvent, SoundEvent> LICH_HURT = SOUNDS.register(
            "lich.hurt",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("lich.hurt")));

    public static final DeferredHolder<SoundEvent, SoundEvent> LICH_DEATH = SOUNDS.register(
            "lich.death",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("lich.death")));

    public static final DeferredHolder<SoundEvent, SoundEvent> LICH_STEP = SOUNDS.register(
            "lich.step",
            () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("lich.step")));
}
