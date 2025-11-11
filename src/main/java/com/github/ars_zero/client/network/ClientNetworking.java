package com.github.ars_zero.client.network;

import com.github.ars_zero.common.network.PacketStaffSpellFired;
import com.github.ars_zero.common.network.PacketUpdateStaffGUI;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ClientNetworking {
    private ClientNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToClient(
            PacketStaffSpellFired.TYPE,
            PacketStaffSpellFired.STREAM_CODEC,
            ClientPacketHandlers::handleStaffSpellFired
        );

        registrar.playToClient(
            PacketUpdateStaffGUI.TYPE,
            PacketUpdateStaffGUI.CODEC,
            ClientPacketHandlers::handleStaffGuiUpdate
        );
    }
}


