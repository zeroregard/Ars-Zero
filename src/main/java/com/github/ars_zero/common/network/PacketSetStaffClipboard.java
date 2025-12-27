package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.spell.StaffSpellClipboard;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.theillusivec4.curios.api.CuriosApi;

public record PacketSetStaffClipboard(CompoundTag clipboardTag, boolean mainHand, boolean circlet)
    implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketSetStaffClipboard> TYPE =
        new CustomPacketPayload.Type<>(ArsZero.prefix("set_staff_clipboard"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSetStaffClipboard> CODEC =
        StreamCodec.ofMember(PacketSetStaffClipboard::toBytes, PacketSetStaffClipboard::new);

    public PacketSetStaffClipboard(RegistryFriendlyByteBuf buf) {
        this(buf.readNbt(), buf.readBoolean(), buf.readBoolean());
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeNbt(clipboardTag);
        buf.writeBoolean(mainHand);
        buf.writeBoolean(circlet);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketSetStaffClipboard packet, IPayloadContext context) {
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

            var clipboardOpt = StaffSpellClipboard.fromTag(packet.clipboardTag());
            if (clipboardOpt.isEmpty()) {
                return;
            }
            StaffSpellClipboard.writeToStack(stack, clipboardOpt.get());
            player.containerMenu.broadcastChanges();
        });
    }
}


