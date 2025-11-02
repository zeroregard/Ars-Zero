package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.client.renderer.StaffDebugHUD;
import com.github.ars_zero.client.sound.StaffSoundManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketStaffSpellFired(int phaseOrdinal) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PacketStaffSpellFired> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("staff_spell_fired"));
    
    public static final StreamCodec<ByteBuf, PacketStaffSpellFired> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketStaffSpellFired::phaseOrdinal,
        PacketStaffSpellFired::new
    );
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(PacketStaffSpellFired packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            AbstractSpellStaff.StaffPhase phase = AbstractSpellStaff.StaffPhase.values()[packet.phaseOrdinal];
            StaffDebugHUD.onSpellFired(phase);
            
            var player = Minecraft.getInstance().player;
            if (player != null) {
                if (phase == AbstractSpellStaff.StaffPhase.BEGIN) {
                    StaffSoundManager.startLoopingSound(player);
                } else if (phase == AbstractSpellStaff.StaffPhase.END) {
                    StaffSoundManager.stopLoopingSound();
                }
            }
        });
    }
}

