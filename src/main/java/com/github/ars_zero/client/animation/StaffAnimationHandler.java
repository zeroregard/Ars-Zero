package com.github.ars_zero.client.animation;

import com.github.ars_zero.ArsZero;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaffAnimationHandler {
    
    public static final ResourceLocation SPELL_STAFF_LAYER_ID = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff");
    public static final ResourceLocation SPELL_STAFF_CAST_ANIM = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff_cast");
    public static final ResourceLocation SPELL_STAFF_RESET_ANIM = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff_reset");
    public static final ResourceLocation SPELL_STAFF_CAST_LEFT_ANIM = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff_cast_left");
    public static final ResourceLocation SPELL_STAFF_RESET_LEFT_ANIM = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff_reset_left");
    
    private static final Map<UUID, Integer> playerTickCountMap = new HashMap<>();
    
    public static void init() {
        ArsZero.LOGGER.info("Initializing Staff Animation Handler");
        
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
            SPELL_STAFF_LAYER_ID,
            1000,
            StaffAnimationHandler::createAnimationController
        );
    }
    
    private static PlayerAnimationController createAnimationController(AbstractClientPlayer player) {
        return new PlayerAnimationController(player, (controller, state, animSetter) -> {
            return PlayState.STOP;
        });
    }
    
    public static void onStaffPhase(AbstractClientPlayer player, boolean isMainHand, String phase, int tickCount) {
        PlayerAnimationController controller = (PlayerAnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(
            player, SPELL_STAFF_LAYER_ID);
        if (controller == null) return;
        
        UUID playerId = player.getUUID();
        
        switch (phase) {
            case "BEGIN" -> {
                ResourceLocation animation = isMainHand ? SPELL_STAFF_CAST_ANIM : SPELL_STAFF_CAST_LEFT_ANIM;
                controller.triggerAnimation(animation);
                playerTickCountMap.put(playerId, 0);
                ArsZero.LOGGER.debug("BEGIN: Playing staff cast animation ({}) for player: {}", isMainHand ? "right" : "left", player.getName().getString());
            }
            case "TICK" -> {
                Integer currentTicks = playerTickCountMap.get(playerId);
                if (currentTicks != null) {
                    playerTickCountMap.put(playerId, currentTicks + 1);
                }
                ArsZero.LOGGER.debug("TICK: Tick {} for player: {}", currentTicks != null ? currentTicks + 1 : "unknown", player.getName().getString());
            }
            case "END" -> {
                Integer totalTicks = playerTickCountMap.remove(playerId);
                if (totalTicks != null) {
                    if (totalTicks >= 5) {
                        // Long hold - play reset animation
                        ResourceLocation animation = isMainHand ? SPELL_STAFF_RESET_ANIM : SPELL_STAFF_RESET_LEFT_ANIM;
                        controller.triggerAnimation(animation);
                        ArsZero.LOGGER.debug("END: Long hold ({} ticks), resetting staff animation ({}) for player: {}", totalTicks, isMainHand ? "right" : "left", player.getName().getString());
                    } else {
                        // Quick click - don't play reset animation, let cast animation naturally return
                        ArsZero.LOGGER.debug("END: Quick click ({} ticks), letting animation naturally return for player: {}", totalTicks, player.getName().getString());
                    }
                }
            }
        }
    }
}