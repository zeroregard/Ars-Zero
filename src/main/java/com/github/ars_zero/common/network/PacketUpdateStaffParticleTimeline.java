package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineMap;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketUpdateStaffParticleTimeline(int hotkeySlot, TimelineMap particles) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PacketUpdateStaffParticleTimeline> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("update_staff_particle_timeline"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateStaffParticleTimeline> CODEC = 
        StreamCodec.ofMember(PacketUpdateStaffParticleTimeline::toBytes, PacketUpdateStaffParticleTimeline::new);
    
    public PacketUpdateStaffParticleTimeline(RegistryFriendlyByteBuf buf) {
        this(buf.readInt(), TimelineMap.STREAM.decode(buf));
    }
    
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(hotkeySlot);
        TimelineMap.STREAM.encode(buf, particles);
    }
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(PacketUpdateStaffParticleTimeline packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack stack = AbstractMultiPhaseCastDevice.findDeviceStack(player);
                
                if (stack.isEmpty() || !(stack.getItem() instanceof AbstractMultiPhaseCastDevice)) {
                    ArsZero.LOGGER.warn("[SERVER] No multi-phase cast device found!");
                    return;
                }
                
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
        });
    }
}

