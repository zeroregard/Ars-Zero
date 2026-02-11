package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.SpellcastingCirclet;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Map;
import java.util.Optional;

public record PacketSwapCircletToHand() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSwapCircletToHand> TYPE =
        new CustomPacketPayload.Type<>(ArsZero.prefix("swap_circlet_to_hand"));

    public static final StreamCodec<ByteBuf, PacketSwapCircletToHand> CODEC =
        StreamCodec.unit(new PacketSwapCircletToHand());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketSwapCircletToHand packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                return;
            }
            Optional<SlotResult> slotResultOpt = CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findCurios(stack -> stack.getItem() instanceof SpellcastingCirclet)
                    .stream()
                    .findFirst());
            if (slotResultOpt.isEmpty()) {
                return;
            }
            SlotResult slotResult = slotResultOpt.get();
            ItemStack circletStack = slotResult.stack();
            if (circletStack.isEmpty() || !(circletStack.getItem() instanceof SpellcastingCirclet)) {
                return;
            }
            String slotId = slotResult.slotContext().identifier();
            int slotIndex = slotResult.slotContext().index();

            Optional<ICuriosItemHandler> invOpt = CuriosApi.getCuriosInventory(player);
            if (invOpt.isEmpty()) {
                return;
            }
            ICuriosItemHandler inv = invOpt.get();
            Map<String, ICurioStacksHandler> curios = inv.getCurios();
            ICurioStacksHandler stacksHandler = curios.get(slotId);
            if (stacksHandler == null) {
                return;
            }
            stacksHandler.getStacks().setStackInSlot(slotIndex, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND, circletStack);

            PacketDistributor.sendToPlayer(player, new PacketOpenCircletGui(new CircletSlotInfo(slotId, slotIndex)));
        });
    }
}
