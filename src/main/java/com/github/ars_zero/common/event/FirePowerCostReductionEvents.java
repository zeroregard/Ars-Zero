package com.github.ars_zero.common.event;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.common.glyph.ConjureVoxelEffect;
import com.github.ars_zero.common.util.SpellDiscountUtil;
import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectIgnite;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;

public class FirePowerCostReductionEvents {

    @SubscribeEvent
    public static void onSpellCostCalcPre(SpellCostCalcEvent.Pre event) {
        applyCostReduction(event);
    }

    @SubscribeEvent
    public static void onSpellCostCalcPost(SpellCostCalcEvent.Post event) {
        applyCostReduction(event);
    }

    private static void applyCostReduction(SpellCostCalcEvent event) {
        if (!(event.context.getCaster() instanceof LivingCaster caster)) {
            return;
        }
        if (!(caster.livingEntity instanceof Player player)) {
            return;
        }
        if (player instanceof FakePlayer) {
            return;
        }
        
        java.util.List<AbstractSpellPart> recipe = event.context.getSpell().recipe();
        AbstractSpellPart prev = null;
        int augmentEffectCost = 0;
        boolean foundPair = false;
        
        for (AbstractSpellPart part : recipe) {
            if (prev instanceof ConjureVoxelEffect) {
                if (part instanceof EffectIgnite) {
                    augmentEffectCost = part.getCastingCost();
                    foundPair = true;
                    break;
                }
            }
            prev = part;
        }
        
        if (foundPair && augmentEffectCost > 0) {
            event.currentCost = Math.max(0, event.currentCost - augmentEffectCost);
            
            AttributeInstance firePower = player.getAttribute(ModRegistry.FIRE_POWER);
            if (firePower == null) {
                return;
            }

            double power = firePower.getValue();
            if (power <= 0) {
                return;
            }

            double reductionPercent = SpellDiscountUtil.computeReductionPercent(power);
            int baseVoxelCost = ConjureVoxelEffect.INSTANCE.getDefaultManaCost();
            int reducibleBase = Math.min(baseVoxelCost, event.currentCost);
            int totalReduction = (int) Math.ceil(reducibleBase * reductionPercent / 100.0);
            event.currentCost = Math.max(0, event.currentCost - totalReduction);
        }
    }

}
