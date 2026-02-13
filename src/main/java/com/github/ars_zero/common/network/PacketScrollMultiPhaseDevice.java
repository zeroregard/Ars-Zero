package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.AbstractGeometryProcessEntity;
import com.github.ars_zero.common.entity.IAltScrollable;
import com.github.ars_zero.common.entity.IDepthScrollable;
import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.spell.IMultiPhaseCaster;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellResult;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketScrollMultiPhaseDevice(double scrollDelta, boolean modifierHeld, boolean depthModifierHeld, Vec3 playerLookDirection) implements CustomPacketPayload {
    private static final double SCROLL_SENSITIVITY = 0.4;
    private static final double MIN_DISTANCE_MULTIPLIER = 0.1;
    private static final double MAX_DISTANCE_MULTIPLIER = 50.0;

    public static final CustomPacketPayload.Type<PacketScrollMultiPhaseDevice> TYPE = new CustomPacketPayload.Type<>(
            ArsZero.prefix("scroll_multiphase_device"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketScrollMultiPhaseDevice> CODEC = StreamCodec.of(
            PacketScrollMultiPhaseDevice::write,
            PacketScrollMultiPhaseDevice::read);

    private static void write(RegistryFriendlyByteBuf buf, PacketScrollMultiPhaseDevice packet) {
        buf.writeDouble(packet.scrollDelta);
        buf.writeBoolean(packet.modifierHeld);
        buf.writeBoolean(packet.depthModifierHeld);
        buf.writeDouble(packet.playerLookDirection.x);
        buf.writeDouble(packet.playerLookDirection.y);
        buf.writeDouble(packet.playerLookDirection.z);
    }

    private static PacketScrollMultiPhaseDevice read(RegistryFriendlyByteBuf buf) {
        double scrollDelta = buf.readDouble();
        boolean modifierHeld = buf.readBoolean();
        boolean depthModifierHeld = buf.readBoolean();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        return new PacketScrollMultiPhaseDevice(scrollDelta, modifierHeld, depthModifierHeld, new Vec3(x, y, z));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketScrollMultiPhaseDevice packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack heldItem = player.getMainHandItem();
                IMultiPhaseCaster caster = AbstractMultiPhaseCastDevice.asMultiPhaseCaster(player, heldItem);
                if (caster == null) {
                    return;
                }
                
                MultiPhaseCastContext castContext = caster.getCastContext();
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

                if (target instanceof AbstractGeometryProcessEntity geometryEntity) {
                    int direction = packet.scrollDelta > 0 ? 1 : -1;
                    BlockPos offset = calculateDepthOffset(direction, packet.playerLookDirection);
                    geometryEntity.addUserOffset(offset);
                    first.userOffset = geometryEntity.getUserOffset();
                } else {
                    double multiplierChange = packet.scrollDelta * SCROLL_SENSITIVITY;
                    castContext.distanceMultiplier += multiplierChange;
                    castContext.distanceMultiplier = Math.max(MIN_DISTANCE_MULTIPLIER,
                            Math.min(MAX_DISTANCE_MULTIPLIER, castContext.distanceMultiplier));
                }
            }
        });
    }

    private static BlockPos calculateDepthOffset(int direction, Vec3 playerLookDirection) {
        double absX = Math.abs(playerLookDirection.x);
        double absY = Math.abs(playerLookDirection.y);
        double absZ = Math.abs(playerLookDirection.z);

        boolean lookingMostlyVertical = absY > Math.max(absX, absZ);

        if (lookingMostlyVertical) {
            int sign = playerLookDirection.y >= 0 ? 1 : -1;
            return new BlockPos(0, sign * direction, 0);
        } else if (absX >= absZ) {
            int sign = playerLookDirection.x >= 0 ? 1 : -1;
            return new BlockPos(sign * direction, 0, 0);
        } else {
            int sign = playerLookDirection.z >= 0 ? 1 : -1;
            return new BlockPos(0, 0, sign * direction);
        }
    }
}
