package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.SpellcastingCirclet;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Optional;

public record PacketPutCircletBack(CircletSlotInfo slot) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketPutCircletBack> TYPE =
        new CustomPacketPayload.Type<>(ArsZero.prefix("put_circlet_back"));

    public static final StreamCodec<ByteBuf, PacketPutCircletBack> CODEC =
        StreamCodec.composite(CircletSlotInfo.CODEC, PacketPutCircletBack::slot, PacketPutCircletBack::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketPutCircletBack packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            CircletSlotInfo slotInfo = packet.slot();

            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack circletToPutBack;
            if (mainHand.getItem() instanceof SpellcastingCirclet) {
                circletToPutBack = mainHand.copy();
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            } else {
                if (!mainHand.isEmpty()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    ItemEntity dropped = player.drop(mainHand, false);
                    if (dropped != null) dropped.setNoPickUpDelay();
                }
                circletToPutBack = findCircletInInventory(player);
                if (circletToPutBack.isEmpty()) return;
            }

            Optional<ICuriosItemHandler> invOpt = CuriosApi.getCuriosInventory(player);
            if (invOpt.isEmpty()) return;
            ICurioStacksHandler stacksHandler = invOpt.get().getCurios().get(slotInfo.slotId());
            if (stacksHandler == null) return;
            var stacks = stacksHandler.getStacks();
            if (slotInfo.slotIndex() < 0 || slotInfo.slotIndex() >= stacks.getSlots()) return;

            ItemStack currentInSlot = stacks.getStackInSlot(slotInfo.slotIndex());
            if (!currentInSlot.isEmpty()) {
                stacks.setStackInSlot(slotInfo.slotIndex(), ItemStack.EMPTY);
                ItemEntity dropped = player.drop(currentInSlot, false);
                if (dropped != null) dropped.setNoPickUpDelay();
            }
            stacks.setStackInSlot(slotInfo.slotIndex(), circletToPutBack);
        });
    }

    private static ItemStack findCircletInInventory(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof SpellcastingCirclet) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
