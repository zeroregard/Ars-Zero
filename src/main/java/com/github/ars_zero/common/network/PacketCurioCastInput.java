package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.event.CurioCastingHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketCurioCastInput(boolean pressed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketCurioCastInput> TYPE =
        new CustomPacketPayload.Type<>(ArsZero.prefix("curio_cast_input"));
    
    public static final StreamCodec<ByteBuf, PacketCurioCastInput> CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        PacketCurioCastInput::pressed,
        PacketCurioCastInput::new
    );
    
    public static void handle(PacketCurioCastInput packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                CurioCastingHandler.handleInput(serverPlayer, packet.pressed());
            }
        });
    }
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
