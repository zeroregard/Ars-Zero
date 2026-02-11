package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> Client: open circlet GUI after swapping; carries slot so client can send put-back on close. */
public record PacketOpenCircletGui(CircletSlotInfo slot) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketOpenCircletGui> TYPE =
        new CustomPacketPayload.Type<>(ArsZero.prefix("open_circlet_gui"));

    public static final StreamCodec<ByteBuf, PacketOpenCircletGui> CODEC =
        StreamCodec.composite(CircletSlotInfo.CODEC, PacketOpenCircletGui::slot, PacketOpenCircletGui::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
