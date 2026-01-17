package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PacketExplosionActivateSound(double x, double y, double z) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PacketExplosionActivateSound> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("explosion_activate_sound"));
    
    public static final StreamCodec<ByteBuf, PacketExplosionActivateSound> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.DOUBLE,
        PacketExplosionActivateSound::x,
        ByteBufCodecs.DOUBLE,
        PacketExplosionActivateSound::y,
        ByteBufCodecs.DOUBLE,
        PacketExplosionActivateSound::z,
        PacketExplosionActivateSound::new
    );
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}


