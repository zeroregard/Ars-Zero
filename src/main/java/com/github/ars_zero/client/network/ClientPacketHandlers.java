package com.github.ars_zero.client.network;

import com.github.ars_zero.client.ScreenShakeManager;
import com.github.ars_zero.client.gui.AbstractMultiPhaseCastDeviceScreen;
import com.github.ars_zero.client.gui.MultiphaseDeviceStylesScreen;
import com.github.ars_zero.client.renderer.StaffDebugHUD;
import com.github.ars_zero.client.sound.StaffSoundManager;
import com.github.ars_zero.common.item.SpellcastingCirclet;
import com.github.ars_zero.client.sound.ExplosionActivateSoundInstance;
import com.github.ars_zero.common.network.PacketExplosionActivateSound;
import com.github.ars_zero.common.network.PacketExplosionShake;
import com.github.ars_zero.common.network.PacketStaffSpellFired;
import com.github.ars_zero.common.spell.SpellPhase;
import com.github.ars_zero.common.network.PacketUpdateStaffGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    static void handleStaffSpellFired(PacketStaffSpellFired packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            SpellPhase phase = SpellPhase.values()[packet.phaseOrdinal()];
            StaffDebugHUD.onSpellFired(phase);

            var player = Minecraft.getInstance().player;
            if (player instanceof AbstractClientPlayer) {
                if (phase == SpellPhase.BEGIN) {
                    StaffSoundManager.startLoopingSound(player);
                } else if (phase == SpellPhase.END) {
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

            ItemStack updatedStack = packet.stack();
            boolean isCirclet = updatedStack.getItem() instanceof SpellcastingCirclet;

            var mainHandStack = player.getMainHandItem();
            var offHandStack = player.getOffhandItem();

            if (mainHandStack.is(updatedStack.getItem())) {
                player.setItemInHand(InteractionHand.MAIN_HAND, updatedStack);
            } else if (offHandStack.is(updatedStack.getItem())) {
                player.setItemInHand(InteractionHand.OFF_HAND, updatedStack);
            } else if (!isCirclet) {
                return;
            }

            var minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof MultiphaseDeviceStylesScreen staffScreen) {
                staffScreen.onStaffUpdated(updatedStack);
                return;
            }

            if (minecraft.screen instanceof AbstractMultiPhaseCastDeviceScreen staffGUI) {
                staffGUI.onBookstackUpdated(updatedStack);
            }
        });
    }

    static void handleExplosionShake(PacketExplosionShake packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ScreenShakeManager.addShake(packet.intensity(), packet.durationTicks());
        });
    }

    static void handleExplosionActivateSound(PacketExplosionActivateSound packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ExplosionActivateSoundInstance soundInstance = new ExplosionActivateSoundInstance(
                    packet.x(), packet.y(), packet.z());
            Minecraft.getInstance().getSoundManager().play(soundInstance);
        });
    }
}
