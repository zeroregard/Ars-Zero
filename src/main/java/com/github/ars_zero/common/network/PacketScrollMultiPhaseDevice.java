package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.AbstractGeometryProcessEntity;
import com.github.ars_zero.common.entity.IAltScrollable;
import com.github.ars_zero.common.entity.IDepthScrollable;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellResult;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketScrollMultiPhaseDevice(double scrollDelta, boolean modifierHeld, boolean depthModifierHeld) implements CustomPacketPayload {
    private static final double SCROLL_SENSITIVITY = 0.4;
    private static final double MIN_DISTANCE_MULTIPLIER = 0.1;
    private static final double MAX_DISTANCE_MULTIPLIER = 50.0;

    public static final CustomPacketPayload.Type<PacketScrollMultiPhaseDevice> TYPE = new CustomPacketPayload.Type<>(
            ArsZero.prefix("scroll_multiphase_device"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketScrollMultiPhaseDevice> CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.DOUBLE,
                    PacketScrollMultiPhaseDevice::scrollDelta,
                    ByteBufCodecs.BOOL,
                    PacketScrollMultiPhaseDevice::modifierHeld,
                    ByteBufCodecs.BOOL,
                    PacketScrollMultiPhaseDevice::depthModifierHeld,
                    PacketScrollMultiPhaseDevice::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketScrollMultiPhaseDevice packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack heldItem = player.getMainHandItem();
                MultiPhaseCastContext castContext = AbstractMultiPhaseCastDevice.findContextByStack(player, heldItem);

                if (castContext == null || !castContext.isCasting) {
                    return;
                }

                if (castContext.beginResults.isEmpty()) {
                    return;
                }

                SpellResult first = castContext.beginResults.get(0);
                Entity target = first != null ? first.targetEntity : null;
                
                if (packet.depthModifierHeld && target instanceof IDepthScrollable depthScrollable) {
                    depthScrollable.handleDepthScroll(packet.scrollDelta);
                    if (target instanceof AbstractGeometryProcessEntity geometryEntity) {
                        first.depth = geometryEntity.getDepth();
                    }
                    return;
                }
                
                if (packet.modifierHeld && target instanceof IAltScrollable scrollable) {
                    scrollable.handleAltScroll(packet.scrollDelta);
                    return;
                }

                if (target instanceof AbstractGeometryProcessEntity) {
                    int direction = packet.scrollDelta > 0 ? 1 : -1;
                    castContext.distanceMultiplier += direction;
                    castContext.distanceMultiplier = Math.max(1.0,
                            Math.min(MAX_DISTANCE_MULTIPLIER, castContext.distanceMultiplier));
                } else {
                    double multiplierChange = packet.scrollDelta * SCROLL_SENSITIVITY;
                    castContext.distanceMultiplier += multiplierChange;
                    castContext.distanceMultiplier = Math.max(MIN_DISTANCE_MULTIPLIER,
                            Math.min(MAX_DISTANCE_MULTIPLIER, castContext.distanceMultiplier));
                }
            }
        });
    }
}
