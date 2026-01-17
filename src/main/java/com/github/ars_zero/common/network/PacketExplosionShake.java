package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PacketExplosionShake(float intensity, int durationTicks) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PacketExplosionShake> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("explosion_shake"));
    
    public static final StreamCodec<ByteBuf, PacketExplosionShake> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT,
        PacketExplosionShake::intensity,
        ByteBufCodecs.VAR_INT,
        PacketExplosionShake::durationTicks,
        PacketExplosionShake::new
    );
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}


