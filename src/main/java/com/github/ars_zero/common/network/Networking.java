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
    }
}

