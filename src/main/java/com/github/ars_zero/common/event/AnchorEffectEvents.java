package com.github.ars_zero.common.event;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.glyph.AnchorEffect;
import com.github.ars_zero.registry.ModAttachments;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.MultiPhaseCastContextMap;
import com.github.ars_zero.common.spell.SpellResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.minecraft.world.damagesource.DamageSource;

@EventBusSubscriber(modid = ArsZero.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class AnchorEffectEvents {
    
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        
        if (!(event.getEntity() instanceof ServerPlayer caster)) {
            return;
        }
        
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player attacker)) {
            return;
        }
        
        if (!(caster.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        MultiPhaseCastContextMap contextMap = caster.getData(ModAttachments.CAST_CONTEXTS);
        if (contextMap == null || contextMap.isEmpty()) {
            return;
        }
        
        for (MultiPhaseCastContext context : contextMap.getAll().values()) {
            if (context == null || context.beginResults.isEmpty()) {
                continue;
            }
            
            for (SpellResult beginResult : context.beginResults) {
                if (beginResult.targetEntity == attacker) {
                    AnchorEffect.restoreEntityPhysics(context);
                    return;
                }
            }
        }
    }
}

