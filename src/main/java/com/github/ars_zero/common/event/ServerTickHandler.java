package com.github.ars_zero.common.event;

import com.github.ars_zero.common.glyph.TranslateEffect;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = "ars_zero")
public class ServerTickHandler {
    
    @SubscribeEvent
    public static void onServerTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // Update all active translations every tick
            TranslateEffect.updateTranslations(serverLevel);
        }
    }
}
