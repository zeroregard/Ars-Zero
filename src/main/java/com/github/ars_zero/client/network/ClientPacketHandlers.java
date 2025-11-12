package com.github.ars_zero.client.network;

import com.github.ars_zero.client.animation.StaffAnimationHandler;
import com.github.ars_zero.client.gui.ArsZeroStaffGUI;
import com.github.ars_zero.client.gui.StaffParticleScreen;
import com.github.ars_zero.client.renderer.StaffDebugHUD;
import com.github.ars_zero.client.sound.StaffSoundManager;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.network.PacketStaffSpellFired;
import com.github.ars_zero.common.network.PacketUpdateStaffGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    static void handleStaffSpellFired(PacketStaffSpellFired packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            AbstractSpellStaff.StaffPhase phase = AbstractSpellStaff.StaffPhase.values()[packet.phaseOrdinal()];
            StaffDebugHUD.onSpellFired(phase);

            var player = Minecraft.getInstance().player;
            if (player instanceof AbstractClientPlayer clientPlayer) {
                String phaseName = phase.name();
                StaffAnimationHandler.onStaffPhase(clientPlayer, packet.isMainHand(), phaseName, packet.tickCount());

                if (phase == AbstractSpellStaff.StaffPhase.BEGIN) {
                    StaffSoundManager.startLoopingSound(player);
                } else if (phase == AbstractSpellStaff.StaffPhase.END) {
                    StaffSoundManager.stopLoopingSound();
                }
            }
        });
    }

    static void handleStaffGuiUpdate(PacketUpdateStaffGUI packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player == null) {
                return;
            }

            var mainHandStack = player.getMainHandItem();
            var offHandStack = player.getOffhandItem();

            if (mainHandStack.is(packet.stack().getItem())) {
                player.setItemInHand(InteractionHand.MAIN_HAND, packet.stack());
            } else if (offHandStack.is(packet.stack().getItem())) {
                player.setItemInHand(InteractionHand.OFF_HAND, packet.stack());
            }

            var minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof StaffParticleScreen staffScreen) {
                staffScreen.onStaffUpdated(packet.stack());
                return;
            }

            if (minecraft.screen instanceof ArsZeroStaffGUI staffGUI) {
                staffGUI.onBookstackUpdated(packet.stack());
            }
        });
    }
}


