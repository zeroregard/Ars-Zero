package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractMultiphaseHandheldDevice;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

public record PacketSetStaffSlot(int logicalSlot, boolean isForCirclet) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PacketSetStaffSlot> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("set_staff_slot"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSetStaffSlot> CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketSetStaffSlot::logicalSlot,
        ByteBufCodecs.BOOL,
        PacketSetStaffSlot::isForCirclet,
        PacketSetStaffSlot::new
    );
    
    @Deprecated
    public PacketSetStaffSlot(int logicalSlot) {
        this(logicalSlot, false);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(PacketSetStaffSlot packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack stack = ItemStack.EMPTY;
                InteractionHand hand = null;
                
                Optional<ItemStack> curioStack = CuriosApi.getCuriosHelper().findEquippedCurio(
                    equipped -> equipped.getItem() instanceof AbstractMultiphaseHandheldDevice,
                    player
                ).map(result -> result.getRight());
                
                ItemStack mainStack = player.getMainHandItem();
                ItemStack offStack = player.getOffhandItem();
                
                if (packet.isForCirclet) {
                    if (curioStack.isPresent()) {
                        stack = curioStack.get();
                    } else {
                        ArsZero.LOGGER.warn("[SERVER] Packet marked for circlet but no circlet found!");
                        return;
                    }
                } else {
                    if (mainStack.getItem() instanceof AbstractMultiphaseHandheldDevice) {
                        stack = mainStack;
                        hand = InteractionHand.MAIN_HAND;
                    } else if (offStack.getItem() instanceof AbstractMultiphaseHandheldDevice) {
                        stack = offStack;
                        hand = InteractionHand.OFF_HAND;
                    } else if (curioStack.isPresent()) {
                        stack = curioStack.get();
                    } else {
                        ArsZero.LOGGER.warn("[SERVER] No staff found in hands or curios!");
                        return;
                    }
                }
                
                AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
                if (caster != null) {
                    AbstractCaster<?> updatedCaster = caster.setCurrentSlot(packet.logicalSlot);
                    updatedCaster.saveToStack(stack);
                    player.containerMenu.broadcastChanges();
                }
            }
        });
    }
}

