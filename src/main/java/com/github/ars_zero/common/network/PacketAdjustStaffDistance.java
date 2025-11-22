package com.github.ars_zero.common.network;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.GrappleTetherEntity;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
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
                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    GrappleTetherEntity existingTether = findExistingTether(serverLevel, player);
                    if (existingTether != null) {
                        double scrollSensitivity = 0.5;
                        float lengthChange = (float)(packet.scrollDelta * scrollSensitivity);
                        float oldLength = existingTether.getMaxLength();
                        float newLength = oldLength + lengthChange;
                        
                        float minLength = 2.0f;
                        float maxLength = 50.0f;
                        newLength = Math.max(minLength, Math.min(maxLength, newLength));
                        existingTether.setMaxLength(newLength);
                        return;
                    }
                }
                
                ItemStack heldItem = player.getMainHandItem();
                MultiPhaseCastContext castContext = AbstractMultiPhaseCastDevice.findContextByStack(player, heldItem);
                
                if (castContext == null || !castContext.isCasting) {
                    return;
                }
                
                if (castContext.beginResults.isEmpty()) {
                    return;
                }
                
                double scrollSensitivity = SCROLL_SENSITIVITY;
                
                var targetEntity = castContext.beginResults.get(0).targetEntity;
                float entitySize = 1.0f;
                
                if (targetEntity instanceof BaseVoxelEntity voxel) {
                    entitySize = voxel.getSize();
                    scrollSensitivity *= entitySize;
                }
                
                double multiplierChange = packet.scrollDelta * scrollSensitivity;
                double oldMultiplier = castContext.distanceMultiplier;
                castContext.distanceMultiplier += multiplierChange;
                
                ArsZero.LOGGER.info("Scroll: voxelSize={}, scrollDelta={}, sensitivity={}, multiplierChange={}, distanceMultiplier: {} -> {}", 
                    entitySize, packet.scrollDelta, scrollSensitivity, multiplierChange, oldMultiplier, castContext.distanceMultiplier);
                
                castContext.distanceMultiplier = Math.max(MIN_DISTANCE_MULTIPLIER, 
                                                          Math.min(MAX_DISTANCE_MULTIPLIER, castContext.distanceMultiplier));
            }
        });
    }
    
    private static GrappleTetherEntity findExistingTether(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player) {
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (entity instanceof GrappleTetherEntity tether) {
                if (tether.getPlayerUUID() != null && tether.getPlayerUUID().equals(player.getUUID())) {
                    return tether;
                }
            }
        }
        return null;
    }
}
