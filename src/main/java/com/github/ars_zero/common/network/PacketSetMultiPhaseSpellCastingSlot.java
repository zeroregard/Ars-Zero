package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
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

public record PacketSetMultiPhaseSpellCastingSlot(int logicalSlot, boolean isForCirclet) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PacketSetMultiPhaseSpellCastingSlot> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("set_multiphase_spell_casting_slot"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSetMultiPhaseSpellCastingSlot> CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketSetMultiPhaseSpellCastingSlot::logicalSlot,
        ByteBufCodecs.BOOL,
        PacketSetMultiPhaseSpellCastingSlot::isForCirclet,
        PacketSetMultiPhaseSpellCastingSlot::new
    );
    
    @Deprecated
    public PacketSetMultiPhaseSpellCastingSlot(int logicalSlot) {
        this(logicalSlot, false);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(PacketSetMultiPhaseSpellCastingSlot packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack stack = ItemStack.EMPTY;
                InteractionHand hand = null;
                
                Optional<ItemStack> curioStack = CuriosApi.getCuriosHelper().findEquippedCurio(
                    equipped -> equipped.getItem() instanceof AbstractMultiPhaseCastDevice,
                    player
                ).map(result -> result.getRight());
                
                ItemStack mainStack = player.getMainHandItem();
                ItemStack offStack = player.getOffhandItem();
                
                if (mainStack.getItem() instanceof AbstractMultiPhaseCastDevice) {
                    stack = mainStack;
                    hand = InteractionHand.MAIN_HAND;
                } else if (offStack.getItem() instanceof AbstractMultiPhaseCastDevice) {
                    stack = offStack;
                    hand = InteractionHand.OFF_HAND;
                } else if (packet.isForCirclet && curioStack.isPresent()) {
                    stack = curioStack.get();
                } else if (!packet.isForCirclet && curioStack.isPresent()) {
                    stack = curioStack.get();
                } else {
                    ArsZero.LOGGER.warn("[SERVER] No multi-phase cast device found for {}!", 
                        packet.isForCirclet ? "circlet" : "staff");
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

