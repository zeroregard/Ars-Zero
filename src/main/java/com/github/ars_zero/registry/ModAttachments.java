package com.github.ars_zero.registry;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.attachment.FrozenPhysicsAttachment;
import com.github.ars_zero.common.attachment.GravitySuppressionAttachment;
import com.github.ars_zero.common.spell.StaffCastContext;
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
    
    public static final Supplier<AttachmentType<FrozenPhysicsAttachment>> FROZEN_PHYSICS = 
        ATTACHMENT_TYPES.register("frozen_physics", () -> 
            AttachmentType.builder(() -> new FrozenPhysicsAttachment()).build()
        );

    public static final Supplier<AttachmentType<GravitySuppressionAttachment>> GRAVITY_SUPPRESSION =
        ATTACHMENT_TYPES.register("gravity_suppression", () ->
            AttachmentType.builder(GravitySuppressionAttachment::new)
                .serialize(GravitySuppressionAttachment.CODEC)
                .copyOnDeath()
                .build()
        );
}

