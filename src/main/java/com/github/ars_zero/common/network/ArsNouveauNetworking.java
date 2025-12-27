package com.github.ars_zero.common.network;

import com.hollingsworth.arsnouveau.common.network.PacketUpdateCaster;

public final class ArsNouveauNetworking {

    private ArsNouveauNetworking() {
    }

    public static void sendToServer(PacketUpdateCaster packet) {
        com.hollingsworth.arsnouveau.common.network.Networking.sendToServer(packet);
    }
}


