package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.theillusivec4.curios.api.CuriosApi;

public record PacketUpdateTickDelay(int logicalSlot, int delay, boolean mainHand, boolean circlet)
    implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketUpdateTickDelay> TYPE =
        new CustomPacketPayload.Type<>(ArsZero.prefix("update_tick_delay"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateTickDelay> CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketUpdateTickDelay::logicalSlot,
        ByteBufCodecs.INT,
        PacketUpdateTickDelay::delay,
        ByteBufCodecs.BOOL,
        PacketUpdateTickDelay::mainHand,
        ByteBufCodecs.BOOL,
        PacketUpdateTickDelay::circlet,
        PacketUpdateTickDelay::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketUpdateTickDelay packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ItemStack stack = ItemStack.EMPTY;
            if (packet.circlet()) {
                stack = CuriosApi.getCuriosHelper()
                    .findEquippedCurio(itemStack -> itemStack.getItem() instanceof AbstractMultiPhaseCastDevice, player)
                    .map(result -> result.getRight())
                    .orElse(ItemStack.EMPTY);
            } else {
                InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                stack = player.getItemInHand(hand);
            }

            if (!(stack.getItem() instanceof AbstractMultiPhaseCastDevice)) {
                return;
            }

            AbstractMultiPhaseCastDevice.setSlotTickDelay(stack, packet.logicalSlot(), packet.delay());
            player.containerMenu.broadcastChanges();
        });
    }
}
