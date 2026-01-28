package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.casting.CastingStyle;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.theillusivec4.curios.api.CuriosApi;

public record PacketUpdateCastingStyle(int hotkeySlot, CastingStyle style, boolean isForCirclet) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PacketUpdateCastingStyle> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("update_casting_style"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateCastingStyle> CODEC = 
        StreamCodec.ofMember(PacketUpdateCastingStyle::toBytes, PacketUpdateCastingStyle::new);
    
    public PacketUpdateCastingStyle(RegistryFriendlyByteBuf buf) {
        this(buf.readInt(), CastingStyle.load(buf.readNbt()), buf.readBoolean());
    }
    
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(hotkeySlot);
        buf.writeNbt(style.save());
        buf.writeBoolean(isForCirclet);
    }
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(PacketUpdateCastingStyle packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack stack = findTargetDeviceStack(player, packet.isForCirclet);
                
                if (stack.isEmpty() || !(stack.getItem() instanceof AbstractMultiPhaseCastDevice)) {
                    ArsZero.LOGGER.warn("[SERVER] No multi-phase cast device found for {}!", 
                        packet.isForCirclet ? "circlet" : "staff");
                    return;
                }
                
                String key = "ars_zero_casting_style_" + packet.hotkeySlot;
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
                
                tag.put(key, packet.style.save());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                
                player.containerMenu.broadcastChanges();
                
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new PacketUpdateStaffGUI(stack));
            }
        });
    }
    
    private static ItemStack findTargetDeviceStack(ServerPlayer player, boolean isForCirclet) {
        ItemStack mainStack = player.getMainHandItem();
        if (mainStack.getItem() instanceof AbstractMultiPhaseCastDevice) {
            return mainStack;
        }
        
        ItemStack offStack = player.getOffhandItem();
        if (offStack.getItem() instanceof AbstractMultiPhaseCastDevice) {
            return offStack;
        }
        
        if (isForCirclet) {
            return CuriosApi.getCuriosHelper()
                .findEquippedCurio(
                    equipped -> equipped.getItem() instanceof AbstractMultiPhaseCastDevice,
                    player
                )
                .map(result -> result.getRight())
                .orElse(ItemStack.EMPTY);
        }
        
        return ItemStack.EMPTY;
    }
}
