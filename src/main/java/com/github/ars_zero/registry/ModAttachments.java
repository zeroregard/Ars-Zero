package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.spell.StaffCastContext;
import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = 
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ArsZero.MOD_ID);

    public static final Supplier<AttachmentType<StaffCastContext>> STAFF_CONTEXT = 
        ATTACHMENT_TYPES.register("staff_context", () -> 
            AttachmentType.<StaffCastContext>builder(() -> null).build()
        );
}

