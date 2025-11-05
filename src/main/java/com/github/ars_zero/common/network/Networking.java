package com.github.ars_zero.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class Networking {
    
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        
        registrar.playToClient(
            PacketStaffSpellFired.TYPE,
            PacketStaffSpellFired.STREAM_CODEC,
            PacketStaffSpellFired::handle
        );
        
        registrar.playToServer(
            PacketSetStaffSlot.TYPE,
            PacketSetStaffSlot.CODEC,
            PacketSetStaffSlot::handle
        );
        
        registrar.playToServer(
            PacketSetStaffSound.TYPE,
            PacketSetStaffSound.CODEC,
            PacketSetStaffSound::handle
        );
        
        registrar.playToServer(
            PacketAdjustStaffDistance.TYPE,
            PacketAdjustStaffDistance.CODEC,
            PacketAdjustStaffDistance::handle
        );
        
        registrar.playToServer(
            PacketUpdateStaffParticleTimeline.TYPE,
            PacketUpdateStaffParticleTimeline.CODEC,
            PacketUpdateStaffParticleTimeline::handle
        );
        
        registrar.playToClient(
            PacketUpdateStaffGUI.TYPE,
            PacketUpdateStaffGUI.CODEC,
            PacketUpdateStaffGUI::handle
        );
    }
    
    public static void sendToServer(PacketSetStaffSlot packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }
    
    public static void sendToServer(PacketSetStaffSound packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }
    
    public static void sendToServer(PacketAdjustStaffDistance packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }
    
    public static void sendToServer(PacketUpdateStaffParticleTimeline packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }
}

