package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractMultiphaseHandheldDevice;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineMap;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.theillusivec4.curios.api.CuriosApi;

public record PacketUpdateMultiphaseDeviceParticleTimeline(int hotkeySlot, TimelineMap particles, boolean isForCirclet) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PacketUpdateMultiphaseDeviceParticleTimeline> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("update_multiphase_device_particle_timeline"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateMultiphaseDeviceParticleTimeline> CODEC = 
        StreamCodec.ofMember(PacketUpdateMultiphaseDeviceParticleTimeline::toBytes, PacketUpdateMultiphaseDeviceParticleTimeline::new);
    
    public PacketUpdateMultiphaseDeviceParticleTimeline(RegistryFriendlyByteBuf buf) {
        this(buf.readInt(), TimelineMap.STREAM.decode(buf), buf.readBoolean());
    }
    
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(hotkeySlot);
        TimelineMap.STREAM.encode(buf, particles);
        buf.writeBoolean(isForCirclet);
    }
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(PacketUpdateMultiphaseDeviceParticleTimeline packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack stack = findTargetDeviceStack(player, packet.isForCirclet);
                
                if (stack.isEmpty() || !(stack.getItem() instanceof AbstractMultiphaseHandheldDevice)) {
                    ArsZero.LOGGER.warn("[SERVER] No multi-phase cast device found for {}!", 
                        packet.isForCirclet ? "circlet" : "staff");
                    return;
                }
                
                AbstractCaster<?> caster = SpellCasterRegistry.from(stack);
                
                if (caster == null) {
                    ArsZero.LOGGER.warn("[SERVER] No caster found on stack!");
                    return;
                }
                
                for (int phase = 0; phase < 3; phase++) {
                    int physicalSlot = packet.hotkeySlot * 3 + phase;
                    caster = caster.setParticles(packet.particles, physicalSlot);
                }
                
                caster.saveToStack(stack);
                player.containerMenu.broadcastChanges();
                
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new PacketUpdateStaffGUI(stack));
            }
        });
    }
    
    private static ItemStack findTargetDeviceStack(ServerPlayer player, boolean isForCirclet) {
        ItemStack mainStack = player.getMainHandItem();
        if (mainStack.getItem() instanceof AbstractMultiphaseHandheldDevice) {
            return mainStack;
        }
        
        ItemStack offStack = player.getOffhandItem();
        if (offStack.getItem() instanceof AbstractMultiphaseHandheldDevice) {
            return offStack;
        }
        
        if (isForCirclet) {
            return CuriosApi.getCuriosHelper()
                .findEquippedCurio(
                    equipped -> equipped.getItem() instanceof AbstractMultiphaseHandheldDevice,
                    player
                )
                .map(result -> result.getRight())
                .orElse(ItemStack.EMPTY);
        }
        
        return ItemStack.EMPTY;
    }
}

