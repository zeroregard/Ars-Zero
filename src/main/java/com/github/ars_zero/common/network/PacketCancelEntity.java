package com.github.ars_zero.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.github.ars_zero.ArsZero;

public record PacketCancelEntity(int entityId) implements CustomPacketPayload {

  public static final Type<PacketCancelEntity> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath(ArsZero.MOD_ID, "cancel_entity"));

  public static final StreamCodec<ByteBuf, PacketCancelEntity> CODEC = StreamCodec.composite(
      ByteBufCodecs.INT,
      PacketCancelEntity::entityId,
      PacketCancelEntity::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(PacketCancelEntity packet, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (context.player() instanceof ServerPlayer serverPlayer) {
        Entity entity = serverPlayer.level().getEntity(packet.entityId);
        if (entity != null) {
          entity.discard();
        }
      }
    });
  }
}

