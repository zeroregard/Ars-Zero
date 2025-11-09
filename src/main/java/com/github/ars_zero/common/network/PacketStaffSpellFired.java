package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PacketStaffSpellFired(int phaseOrdinal, boolean isMainHand, int tickCount) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PacketStaffSpellFired> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("staff_spell_fired"));
    
    public static final StreamCodec<ByteBuf, PacketStaffSpellFired> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketStaffSpellFired::phaseOrdinal,
        ByteBufCodecs.BOOL,
        PacketStaffSpellFired::isMainHand,
        ByteBufCodecs.INT,
        PacketStaffSpellFired::tickCount,
        PacketStaffSpellFired::new
    );
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

