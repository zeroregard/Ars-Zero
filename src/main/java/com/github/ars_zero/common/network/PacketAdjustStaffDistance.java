package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.terrain.ConjureTerrainConvergenceEntity;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketAdjustStaffDistance(double scrollDelta) implements CustomPacketPayload {
    
    private static final double SCROLL_SENSITIVITY = 0.4;
    private static final double MIN_DISTANCE_MULTIPLIER = 0.1;
    private static final double MAX_DISTANCE_MULTIPLIER = 50.0;
    
    public static final CustomPacketPayload.Type<PacketAdjustStaffDistance> TYPE = 
        new CustomPacketPayload.Type<>(ArsZero.prefix("adjust_staff_distance"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketAdjustStaffDistance> CODEC = StreamCodec.composite(
        ByteBufCodecs.DOUBLE,
        PacketAdjustStaffDistance::scrollDelta,
        PacketAdjustStaffDistance::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(PacketAdjustStaffDistance packet, IPayloadContext context) {
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
                if (target instanceof ConjureTerrainConvergenceEntity terrain && terrain.getLifespan() > 0
                        && !terrain.isBuilding()) {
                    int direction = packet.scrollDelta > 0 ? 1 : (packet.scrollDelta < 0 ? -1 : 0);
                    terrain.adjustSizeStep(direction);
                    return;
                }
                
                double scrollSensitivity = SCROLL_SENSITIVITY;
                
                double multiplierChange = packet.scrollDelta * scrollSensitivity;
                castContext.distanceMultiplier += multiplierChange;
                
                castContext.distanceMultiplier = Math.max(MIN_DISTANCE_MULTIPLIER, 
                                                          Math.min(MAX_DISTANCE_MULTIPLIER, castContext.distanceMultiplier));
            }
        });
    }
}
