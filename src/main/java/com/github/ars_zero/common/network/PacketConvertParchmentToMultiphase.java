package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.registry.ModItems;
import com.github.ars_zero.common.spell.StaffSpellClipboard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketConvertParchmentToMultiphase(CompoundTag clipboardTag, boolean parchmentInMainHand)
    implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketConvertParchmentToMultiphase> TYPE =
        new CustomPacketPayload.Type<>(ArsZero.prefix("convert_parchment_to_multiphase"));

    private static final ResourceLocation SPELL_PARCHMENT_ID = ResourceLocation.fromNamespaceAndPath("ars_nouveau", "spell_parchment");

    public static boolean isSpellParchment(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(SPELL_PARCHMENT_ID);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketConvertParchmentToMultiphase> CODEC =
        StreamCodec.ofMember(PacketConvertParchmentToMultiphase::toBytes, PacketConvertParchmentToMultiphase::new);

    public PacketConvertParchmentToMultiphase(RegistryFriendlyByteBuf buf) {
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

    public static void handle(PacketConvertParchmentToMultiphase packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            InteractionHand hand = packet.parchmentInMainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack inHand = player.getItemInHand(hand);

            if (inHand.isEmpty() || !BuiltInRegistries.ITEM.getKey(inHand.getItem()).equals(SPELL_PARCHMENT_ID)) {
                return;
            }

            var clipboardOpt = StaffSpellClipboard.fromTag(packet.clipboardTag());
            if (clipboardOpt.isEmpty()) {
                return;
            }

            ItemStack multiphase = new ItemStack(ModItems.MULTIPHASE_SPELL_PARCHMENT.get(), 1);
            StaffSpellClipboard.writeToStack(multiphase, clipboardOpt.get(), StaffSpellClipboard.PARCHMENT_SLOT_KEY);

            inHand.shrink(1);
            if (inHand.isEmpty()) {
                player.setItemInHand(hand, multiphase);
            } else {
                player.setItemInHand(hand, inHand);
                if (!player.getInventory().add(multiphase)) {
                    player.drop(multiphase, false);
                }
            }
            player.containerMenu.broadcastChanges();
        });
    }
}
