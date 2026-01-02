package com.github.ars_zero.common.network;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class Networking {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
            PacketSetMultiPhaseSpellCastingSlot.TYPE,
            PacketSetMultiPhaseSpellCastingSlot.CODEC,
            PacketSetMultiPhaseSpellCastingSlot::handle
        );

        registrar.playToServer(
            PacketSetStaffClipboard.TYPE,
            PacketSetStaffClipboard.CODEC,
            PacketSetStaffClipboard::handle
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
            PacketUpdateMultiphaseDeviceParticleTimeline.TYPE,
            PacketUpdateMultiphaseDeviceParticleTimeline.CODEC,
            PacketUpdateMultiphaseDeviceParticleTimeline::handle
        );

        registrar.playToServer(
            PacketUpdateTickDelay.TYPE,
            PacketUpdateTickDelay.CODEC,
            PacketUpdateTickDelay::handle
        );
        
        registrar.playToServer(
            PacketCurioCastInput.TYPE,
            PacketCurioCastInput.CODEC,
            PacketCurioCastInput::handle
        );
        
        if (FMLEnvironment.dist.isDedicatedServer()) {
            registrar.playToClient(
                PacketStaffSpellFired.TYPE,
                PacketStaffSpellFired.STREAM_CODEC,
                (packet, context) -> {}
            );
            
            registrar.playToClient(
                PacketUpdateStaffGUI.TYPE,
                PacketUpdateStaffGUI.CODEC,
                (packet, context) -> {}
            );
            
            registrar.playToClient(
                PacketExplosionShake.TYPE,
                PacketExplosionShake.STREAM_CODEC,
                (packet, context) -> {}
            );
            
            registrar.playToClient(
                com.github.ars_zero.common.network.PacketExplosionActivateSound.TYPE,
                com.github.ars_zero.common.network.PacketExplosionActivateSound.STREAM_CODEC,
                (packet, context) -> {}
            );
        }
    }

    public static void sendToServer(PacketSetMultiPhaseSpellCastingSlot packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    public static void sendToServer(PacketSetStaffClipboard packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    public static void sendToServer(PacketSetStaffSound packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    public static void sendToServer(PacketAdjustStaffDistance packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    public static void sendToServer(PacketUpdateMultiphaseDeviceParticleTimeline packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    public static void sendToServer(PacketUpdateTickDelay packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }
    
    public static void sendToServer(PacketCurioCastInput packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }
}

