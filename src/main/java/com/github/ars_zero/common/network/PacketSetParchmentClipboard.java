package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.MultiphaseSpellParchment;
import com.github.ars_zero.common.spell.StaffSpellClipboard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketSetParchmentClipboard(CompoundTag clipboardTag, boolean parchmentInMainHand)
    implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketSetParchmentClipboard> TYPE =
        new CustomPacketPayload.Type<>(ArsZero.prefix("set_parchment_clipboard"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSetParchmentClipboard> CODEC =
        StreamCodec.ofMember(PacketSetParchmentClipboard::toBytes, PacketSetParchmentClipboard::new);

    public PacketSetParchmentClipboard(RegistryFriendlyByteBuf buf) {
        this(buf.readNbt(), buf.readBoolean());
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeNbt(clipboardTag);
        buf.writeBoolean(parchmentInMainHand);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketSetParchmentClipboard packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            InteractionHand hand = packet.parchmentInMainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);

            if (!(stack.getItem() instanceof MultiphaseSpellParchment)) {
                return;
            }

            var clipboardOpt = StaffSpellClipboard.fromTag(packet.clipboardTag());
            if (clipboardOpt.isEmpty()) {
                return;
            }
            StaffSpellClipboard.writeToStack(stack, clipboardOpt.get(), StaffSpellClipboard.PARCHMENT_SLOT_KEY);
            player.setItemInHand(hand, stack);
            player.containerMenu.broadcastChanges();
        });
    }
}
