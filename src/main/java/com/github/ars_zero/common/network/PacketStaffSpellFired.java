package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.animation.StaffAnimationHandler;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.client.renderer.StaffDebugHUD;
import com.github.ars_zero.client.sound.StaffSoundManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketStaffSpellFired(int phaseOrdinal, boolean isMainHand, int tickCount) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PacketStaffSpellFired> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("staff_spell_fired"));
    
    public static final StreamCodec<ByteBuf, PacketStaffSpellFired> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketStaffSpellFired::phaseOrdinal,
        ByteBufCodecs.BOOL,
        PacketStaffSpellFired::isMainHand,
        ByteBufCodecs.INT,
        PacketStaffSpellFired::tickCount,
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
            if (player instanceof AbstractClientPlayer clientPlayer) {
                String phaseName = phase.name();
                StaffAnimationHandler.onStaffPhase(clientPlayer, packet.isMainHand, phaseName, packet.tickCount);
                
                if (phase == AbstractSpellStaff.StaffPhase.BEGIN) {
                    StaffSoundManager.startLoopingSound(player);
                } else if (phase == AbstractSpellStaff.StaffPhase.END) {
                    StaffSoundManager.stopLoopingSound();
                }
            }
        });
    }
}

