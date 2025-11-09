package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

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
}

