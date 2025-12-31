package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ArsZero.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> EFFECT_PUSH = SOUNDS.register(
        "effect_push", 
        () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("effect_push"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> EFFECT_ANCHOR = SOUNDS.register(
        "effect_anchor", 
        () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("effect_anchor"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHTNING_VOXEL_HIT = SOUNDS.register(
        "lightning_voxel_hit", 
        () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("lightning_voxel_hit"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_CHARGE = SOUNDS.register(
        "explosion_charge", 
        () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("explosion_charge"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_IDLE = SOUNDS.register(
        "explosion_idle", 
        () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("explosion_idle"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_ACTIVATE = SOUNDS.register(
        "explosion_activate", 
        () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("explosion_activate"))
    );
}
