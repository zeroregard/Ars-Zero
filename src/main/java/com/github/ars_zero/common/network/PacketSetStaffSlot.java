package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractSpellStaff;
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

public record PacketSetStaffSlot(int logicalSlot) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PacketSetStaffSlot> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("set_staff_slot"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSetStaffSlot> CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketSetStaffSlot::logicalSlot,
        PacketSetStaffSlot::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(PacketSetStaffSlot packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                InteractionHand hand = null;
                ItemStack mainStack = player.getMainHandItem();
                ItemStack offStack = player.getOffhandItem();
                
                ItemStack stack;
                if (mainStack.getItem() instanceof AbstractSpellStaff) {
                    stack = mainStack;
                    hand = InteractionHand.MAIN_HAND;
                } else if (offStack.getItem() instanceof AbstractSpellStaff) {
                    stack = offStack;
                    hand = InteractionHand.OFF_HAND;
                } else {
                    ArsZero.LOGGER.warn("[SERVER] No staff found in hands!");
                    return;
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

