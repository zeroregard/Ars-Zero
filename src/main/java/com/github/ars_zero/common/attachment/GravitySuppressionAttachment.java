package com.github.ars_zero.common.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class GravitySuppressionAttachment {
    public static final GravitySuppressionAttachment INACTIVE = new GravitySuppressionAttachment(false, false, false, 0L);

    public static final Codec<GravitySuppressionAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("active").forGetter(GravitySuppressionAttachment::isActive),
        Codec.BOOL.fieldOf("useCustomSetter").forGetter(GravitySuppressionAttachment::useCustomSetter),
        Codec.BOOL.fieldOf("originalNoGravity").forGetter(GravitySuppressionAttachment::originalNoGravity),
        Codec.LONG.fieldOf("expireTick").forGetter(GravitySuppressionAttachment::expireTick)
    ).apply(instance, GravitySuppressionAttachment::new));

    private final boolean active;
    private final boolean useCustomSetter;
    private final boolean originalNoGravity;
    private final long expireTick;

    public GravitySuppressionAttachment() {
        this(false, false, false, 0L);
    }

    public GravitySuppressionAttachment(boolean active, boolean useCustomSetter, boolean originalNoGravity, long expireTick) {
        this.active = active;
        this.useCustomSetter = useCustomSetter;
        this.originalNoGravity = originalNoGravity;
        this.expireTick = expireTick;
    }

    public boolean isActive() {
        return active;
    }

    public boolean useCustomSetter() {
        return useCustomSetter;
    }

    public boolean originalNoGravity() {
        return originalNoGravity;
    }

    public long expireTick() {
        return expireTick;
    }

    public GravitySuppressionAttachment activated(boolean useCustomSetter, boolean originalNoGravity, long expireTick) {
        return new GravitySuppressionAttachment(true, useCustomSetter, originalNoGravity, expireTick);
    }

    public GravitySuppressionAttachment extend(boolean nowRequiresCustomSetter, long expireTick) {
        return new GravitySuppressionAttachment(
            true,
            this.useCustomSetter || nowRequiresCustomSetter,
            this.originalNoGravity,
            Math.max(this.expireTick, expireTick)
        );
    }

    public GravitySuppressionAttachment deactivate() {
        return INACTIVE;
    }
}

