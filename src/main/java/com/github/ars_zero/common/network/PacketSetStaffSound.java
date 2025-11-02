package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.hollingsworth.arsnouveau.api.sound.ConfiguredSpellSound;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class PacketSetStaffSound implements CustomPacketPayload {
    public static final Type<PacketSetStaffSound> TYPE = new Type<>(ArsZero.prefix("set_staff_sound"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSetStaffSound> CODEC = StreamCodec.ofMember(PacketSetStaffSound::toBytes, PacketSetStaffSound::new);

    ConfiguredSpellSound beginSound;
    ConfiguredSpellSound tickSound;
    ConfiguredSpellSound endSound;
    ResourceLocation tickLoopingSoundId;
    boolean mainHand;

    public PacketSetStaffSound(ConfiguredSpellSound beginSound, ConfiguredSpellSound tickSound, ConfiguredSpellSound endSound, ResourceLocation tickLoopingSoundId, boolean mainHand) {
        this.beginSound = beginSound;
        this.tickSound = tickSound;
        this.endSound = endSound;
        this.tickLoopingSoundId = tickLoopingSoundId;
        this.mainHand = mainHand;
    }

    public PacketSetStaffSound(RegistryFriendlyByteBuf buf) {
        beginSound = ConfiguredSpellSound.STREAM.decode(buf);
        tickSound = ConfiguredSpellSound.STREAM.decode(buf);
        endSound = ConfiguredSpellSound.STREAM.decode(buf);
        // boolean hasLoopingSound = buf.readBoolean();
        // if (hasLoopingSound) {
        //     tickLoopingSoundId = buf.readResourceLocation();
        // }
        mainHand = buf.readBoolean();
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        ConfiguredSpellSound.STREAM.encode(buf, beginSound);
        ConfiguredSpellSound.STREAM.encode(buf, tickSound);
        ConfiguredSpellSound.STREAM.encode(buf, endSound);
        // buf.writeBoolean(tickLoopingSoundId != null);
        // if (tickLoopingSoundId != null) {
        //     buf.writeResourceLocation(tickLoopingSoundId);
        // }
        buf.writeBoolean(mainHand);
    }

    public void onServerReceived(MinecraftServer minecraftServer, ServerPlayer player) {
        ItemStack stack = player.getItemInHand(mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (stack.getItem() instanceof AbstractSpellStaff) {
            AbstractSpellStaff.setStaffSounds(stack, beginSound, tickSound, endSound, tickLoopingSoundId);
        }
    }

    public static void handle(PacketSetStaffSound packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                packet.onServerReceived(serverPlayer.getServer(), serverPlayer);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

