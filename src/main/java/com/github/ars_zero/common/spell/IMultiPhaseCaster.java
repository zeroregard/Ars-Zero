package com.github.ars_zero.common.spell;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.EntitySpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.IWrappedCaster;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

public interface IMultiPhaseCaster {
    
    static IMultiPhaseCaster from(IWrappedCaster wrappedCaster, Player player, ItemStack casterTool) {
        BlockEntity tile = getBlockEntityFromCaster(wrappedCaster);
        if (tile instanceof IMultiPhaseCaster multiPhaseCaster) {
            return multiPhaseCaster;
        }
        
        if (player != null && !casterTool.isEmpty()) {
            return AbstractMultiPhaseCastDevice.asMultiPhaseCaster(player, casterTool);
        }
        return null;
    }
    
    private static BlockEntity getBlockEntityFromCaster(IWrappedCaster wrappedCaster) {
        try {
            java.lang.reflect.Method getTileMethod = wrappedCaster.getClass().getMethod("getTile");
            return (BlockEntity) getTileMethod.invoke(wrappedCaster);
        } catch (Exception ignored) {
            return null;
        }
    }
    
    static IMultiPhaseCaster from(com.hollingsworth.arsnouveau.api.spell.SpellContext spellContext, LivingEntity shooter) {
        IWrappedCaster wrappedCaster = spellContext.getCaster();
        Player player = shooter instanceof Player ? (Player) shooter : null;
        ItemStack casterTool = spellContext.getCasterTool();
        return from(wrappedCaster, player, casterTool);
    }
    
    MultiPhaseCastContext getCastContext();
    
    UUID getPlayerId();
    
    default SpellResolver wrapResolverForPhase(SpellResolver resolver, SpellPhase phase) {
        MultiPhaseCastContext context = getCastContext();
        UUID playerId = getPlayerId();
        
        if (context != null && playerId != null) {
            if (phase == SpellPhase.BEGIN || phase == SpellPhase.TICK || phase == SpellPhase.END) {
                boolean isRoot = phase == SpellPhase.BEGIN;
                if (resolver instanceof EntitySpellResolver entityResolver) {
                    return new WrappedSpellResolver(entityResolver, playerId, phase, isRoot);
                } else {
                    return new WrappedSpellResolver(resolver, playerId, phase, isRoot);
                }
            }
        } else {
            ArsZero.LOGGER.warn("[IMultiPhaseCaster] Not wrapping resolver: context={}, playerId={}", 
                context != null, playerId != null);
        }
        
        return resolver;
    }
    
    default void updateContextPhase(SpellPhase phase) {
        MultiPhaseCastContext context = getCastContext();
        if (context == null) {
            ArsZero.LOGGER.warn("[IMultiPhaseCaster] updateContextPhase: context is null for phase={}", phase);
            return;
        }
        
        context.currentPhase = phase;
        if (phase == SpellPhase.TICK) {
            context.tickCount++;
            context.sequenceTick++;
        }
    }
}
