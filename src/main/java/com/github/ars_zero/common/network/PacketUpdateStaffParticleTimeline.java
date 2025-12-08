package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineMap;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

public record PacketUpdateStaffParticleTimeline(int hotkeySlot, TimelineMap particles, boolean mainHand, boolean isForCirclet) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PacketUpdateStaffParticleTimeline> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("update_staff_particle_timeline"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateStaffParticleTimeline> CODEC = 
        StreamCodec.ofMember(PacketUpdateStaffParticleTimeline::toBytes, PacketUpdateStaffParticleTimeline::new);
    
    public PacketUpdateStaffParticleTimeline(RegistryFriendlyByteBuf buf) {
        this(buf.readInt(), TimelineMap.STREAM.decode(buf), buf.readBoolean(), buf.readBoolean());
    }
    
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(hotkeySlot);
        TimelineMap.STREAM.encode(buf, particles);
        buf.writeBoolean(mainHand);
        buf.writeBoolean(isForCirclet);
    }
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(PacketUpdateStaffParticleTimeline packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack stack = ItemStack.EMPTY;
                
                if (packet.isForCirclet) {
                    Optional<ItemStack> curioStack = CuriosApi.getCuriosHelper().findEquippedCurio(
                        equipped -> equipped.getItem() instanceof AbstractMultiPhaseCastDevice,
                        player
                    ).map(result -> result.getRight());
                    
                    if (curioStack.isPresent()) {
                        stack = curioStack.get();
                    } else {
                        ArsZero.LOGGER.warn("[SERVER] Packet marked for circlet but no circlet found!");
                        return;
                    }
                } else {
                    stack = player.getItemInHand(packet.mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                }
                
                if (stack.getItem() instanceof AbstractSpellStaff || stack.getItem() instanceof AbstractMultiPhaseCastDevice) {
                    AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
                    
                    if (caster != null) {
                        for (int phase = 0; phase < 3; phase++) {
                            int physicalSlot = packet.hotkeySlot * 3 + phase;
                            caster = caster.setParticles(packet.particles, physicalSlot);
                        }
                        
                        caster.saveToStack(stack);
                        
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new PacketUpdateStaffGUI(stack));
                    }
                }
            }
        });
    }
}

