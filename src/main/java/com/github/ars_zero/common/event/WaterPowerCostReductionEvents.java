package com.github.ars_zero.common.event;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.glyph.ConjureVoxelEffect;
import com.github.ars_zero.common.util.SpellDiscountUtil;
import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectColdSnap;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;

public class WaterPowerCostReductionEvents {

    @SubscribeEvent
    public static void onSpellCostCalcPre(SpellCostCalcEvent.Pre event) {
        applyCostReduction(event);
    }

    @SubscribeEvent
    public static void onSpellCostCalcPost(SpellCostCalcEvent.Post event) {
        applyCostReduction(event);
    }

    private static void applyCostReduction(SpellCostCalcEvent event) {
        if (event.context.getCaster() instanceof LivingCaster caster) {
            if (caster.livingEntity instanceof Player player && !(player instanceof FakePlayer)) {
                int adjacentPairCost = SpellDiscountUtil.computeAdjacentPairCost(event.context.getSpell().recipe(), List.of(EffectConjureWater.class, EffectColdSnap.class));
                if (adjacentPairCost > 0) {
                    AttributeInstance waterPower = player.getAttribute(ModRegistry.WATER_POWER);
                    if (waterPower != null) {
                        double power = waterPower.getValue();
                        if (power > 0) {
                            double reductionPercent = SpellDiscountUtil.computeReductionPercent(power);
                            
                            int reducibleBase = Math.min(adjacentPairCost, event.currentCost);
                            int totalReduction = (int) Math.ceil(reducibleBase * reductionPercent / 100.0);
                            event.currentCost = Math.max(0, event.currentCost - totalReduction);
                        }
                    }
                }
            }
        }
    }

}





