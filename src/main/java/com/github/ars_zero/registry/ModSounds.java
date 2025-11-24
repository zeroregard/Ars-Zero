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
    
    public static final DeferredHolder<SoundEvent, SoundEvent> FUSION_RECORD = SOUNDS.register(
        "fusion_record",
        () -> SoundEvent.createVariableRangeEvent(ArsZero.prefix("fusion_record"))
    );
}
