package com.github.ars_zero.client.animation;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.client.gui.ArsZeroStaffGUI;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaffAnimationHandler {
    
    public static final ResourceLocation SPELL_STAFF_LAYER_ID = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff");
    public static final ResourceLocation SPELL_STAFF_CAST_ANIM = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff_cast");
    public static final ResourceLocation SPELL_STAFF_RESET_ANIM = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff_reset");
    public static final ResourceLocation SPELL_STAFF_CAST_LEFT_ANIM = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff_cast_left");
    public static final ResourceLocation SPELL_STAFF_RESET_LEFT_ANIM = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff_reset_left");
    public static final ResourceLocation SPELL_STAFF_GUI_ANIM = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff_gui");
    public static final ResourceLocation SPELL_STAFF_GUI_LEFT_ANIM = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff_gui_left");
    public static final ResourceLocation SPELL_STAFF_GUI_CLOSE_ANIM = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff_gui_close");
    public static final ResourceLocation SPELL_STAFF_GUI_CLOSE_LEFT_ANIM = ResourceLocation.parse(ArsZero.MOD_ID + ":spell_staff_gui_close_left");
    
    private static final Map<UUID, Integer> playerTickCountMap = new HashMap<>();
    
    public static void init() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
            SPELL_STAFF_LAYER_ID,
            1000,
            StaffAnimationHandler::createAnimationController
        );
    }
    
    private static PlayerAnimationController createAnimationController(AbstractClientPlayer player) {
        return new PlayerAnimationController(player, (controller, state, animSetter) -> {
            if (player == null) {
                return PlayState.STOP;
            }
            
            Minecraft mc = Minecraft.getInstance();
            
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean isMainHand = mainHand.getItem() instanceof AbstractSpellStaff;
            boolean isOffHand = offHand.getItem() instanceof AbstractSpellStaff;
            
            if (!isMainHand && !isOffHand) {
                return PlayState.STOP;
            }
            
            if (mc.screen instanceof ArsZeroStaffGUI) {
                return PlayState.CONTINUE;
            }
            
            if (isMainHand) {
                MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.findContextByStack(player, mainHand);
                if (context != null && context.isCasting && context.source == MultiPhaseCastContext.CastSource.ITEM) {
                    return PlayState.CONTINUE;
                }
            }
            
            if (isOffHand) {
                MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.findContextByStack(player, offHand);
                if (context != null && context.isCasting && context.source == MultiPhaseCastContext.CastSource.ITEM) {
                    return PlayState.CONTINUE;
                }
            }
            
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
            }
            case "TICK" -> {
                Integer currentTicks = playerTickCountMap.get(playerId);
                if (currentTicks != null) {
                    playerTickCountMap.put(playerId, currentTicks + 1);
                }
            }
            case "END" -> {
                Integer totalTicks = playerTickCountMap.remove(playerId);
                // Always play reset animation regardless of tick count
                ResourceLocation animation = isMainHand ? SPELL_STAFF_RESET_ANIM : SPELL_STAFF_RESET_LEFT_ANIM;
                controller.triggerAnimation(animation);
            }
        }
    }
    
    public static void onGuiOpen(AbstractClientPlayer player, boolean isMainHand) {
        PlayerAnimationController controller = (PlayerAnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(
            player, SPELL_STAFF_LAYER_ID);
        if (controller != null) {
            ResourceLocation animation = isMainHand ? SPELL_STAFF_GUI_ANIM : SPELL_STAFF_GUI_LEFT_ANIM;
            controller.triggerAnimation(animation);
        }
    }
    
    public static void onGuiClose(AbstractClientPlayer player, boolean isMainHand) {
        PlayerAnimationController controller = (PlayerAnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(
            player, SPELL_STAFF_LAYER_ID);
        if (controller != null) {
            ResourceLocation animation = isMainHand ? SPELL_STAFF_GUI_CLOSE_ANIM : SPELL_STAFF_GUI_CLOSE_LEFT_ANIM;
            controller.triggerAnimation(animation);
        }
    }
}