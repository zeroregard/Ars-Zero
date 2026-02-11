package com.github.ars_zero.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Curio slot (identifier + index) for swap/put-back; used in packets and GUI. */
public record CircletSlotInfo(String slotId, int slotIndex) {
    public static final StreamCodec<ByteBuf, CircletSlotInfo> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        CircletSlotInfo::slotId,
        ByteBufCodecs.INT,
        CircletSlotInfo::slotIndex,
        CircletSlotInfo::new
    );
}
