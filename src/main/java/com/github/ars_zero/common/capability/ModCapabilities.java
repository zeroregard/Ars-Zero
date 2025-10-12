package com.github.ars_zero.common.capability;

import com.github.ars_zero.ArsZero;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ArsZero.MOD_ID)
public class ModCapabilities {
    
    public static final EntityCapability<IPlayerTranslationCapability, Void> TRANSLATION_CAPABILITY = 
        EntityCapability.createVoid(ArsZero.prefix("translation"), IPlayerTranslationCapability.class);
    
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ArsZero.LOGGER.info("Registering player translation capability");
        event.registerEntity(TRANSLATION_CAPABILITY, EntityType.PLAYER, (player, ctx) -> new PlayerTranslationCapability());
    }
    
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Copy translation capability data when player respawns
        if (!event.isWasDeath()) {
            IPlayerTranslationCapability oldCap = event.getOriginal().getCapability(TRANSLATION_CAPABILITY);
            IPlayerTranslationCapability newCap = event.getEntity().getCapability(TRANSLATION_CAPABILITY);
            
            if (oldCap != null && newCap != null) {
                // Copy the data from old to new
                newCap.setTargetEntity(oldCap.getTargetEntity());
                newCap.setInitialRelativePos(oldCap.getInitialRelativePos());
                newCap.setInitialYaw(oldCap.getInitialYaw());
                newCap.setInitialPitch(oldCap.getInitialPitch());
                newCap.setDuration(oldCap.getDuration());
                newCap.setRemainingTicks(oldCap.getRemainingTicks());
            }
        }
    }
}
