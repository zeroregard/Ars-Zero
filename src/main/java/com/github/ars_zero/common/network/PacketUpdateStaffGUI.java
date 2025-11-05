package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketUpdateStaffGUI(ItemStack stack) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PacketUpdateStaffGUI> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("update_staff_gui"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateStaffGUI> CODEC = 
        StreamCodec.ofMember(PacketUpdateStaffGUI::toBytes, PacketUpdateStaffGUI::new);
    
    public PacketUpdateStaffGUI(RegistryFriendlyByteBuf buf) {
        this(ItemStack.STREAM_CODEC.decode(buf));
    }
    
    public void toBytes(RegistryFriendlyByteBuf buf) {
        ItemStack.STREAM_CODEC.encode(buf, stack);
    }
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(PacketUpdateStaffGUI packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player != null) {
                ItemStack mainHandStack = player.getMainHandItem();
                ItemStack offHandStack = player.getOffhandItem();
                
                if (mainHandStack.is(packet.stack.getItem())) {
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, packet.stack);
                } else if (offHandStack.is(packet.stack.getItem())) {
                    player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, packet.stack);
                }
                
                var minecraft = net.minecraft.client.Minecraft.getInstance();
                if (minecraft.screen instanceof com.github.ars_zero.client.gui.StaffParticleScreen staffScreen) {
                    staffScreen.onStaffUpdated(packet.stack);
                } else if (minecraft.screen instanceof com.github.ars_zero.client.gui.ArsZeroStaffGUI staffGUI) {
                    staffGUI.onBookstackUpdated(packet.stack);
                }
            }
        });
    }
}

