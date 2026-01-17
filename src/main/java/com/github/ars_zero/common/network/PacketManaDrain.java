package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record PacketManaDrain(double amount) implements CustomPacketPayload {

  public static final Type<PacketManaDrain> TYPE = new Type<>(ArsZero.prefix("mana_drain"));

  public static final StreamCodec<RegistryFriendlyByteBuf, PacketManaDrain> STREAM_CODEC = StreamCodec.of(
      (buf, packet) -> buf.writeDouble(packet.amount),
      buf -> new PacketManaDrain(buf.readDouble()));

  @Override
  public @NotNull Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
