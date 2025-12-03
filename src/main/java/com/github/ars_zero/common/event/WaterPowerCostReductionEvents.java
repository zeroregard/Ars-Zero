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
                java.util.List<AbstractSpellPart> recipe = event.context.getSpell().recipe();
                AbstractSpellPart prev = null;
                int augmentEffectCost = 0;
                boolean foundPair = false;
                
                for (AbstractSpellPart part : recipe) {
                    if (prev instanceof ConjureVoxelEffect) {
                        if (part instanceof EffectConjureWater || part instanceof EffectColdSnap) {
                            augmentEffectCost = part.getCastingCost();
                            foundPair = true;
                            break;
                        }
                    }
                    prev = part;
                }
                
                if (foundPair && augmentEffectCost > 0) {
                    event.currentCost = Math.max(0, event.currentCost - augmentEffectCost);
                    
                    AttributeInstance waterPower = player.getAttribute(ModRegistry.WATER_POWER);
                    if (waterPower != null) {
                        double power = waterPower.getValue();
                        if (power > 0) {
                            double reductionPercent = SpellDiscountUtil.computeReductionPercent(power);
                            int baseVoxelCost = ConjureVoxelEffect.INSTANCE.getDefaultManaCost();
                            int reducibleBase = Math.min(baseVoxelCost, event.currentCost);
                            int totalReduction = (int) Math.ceil(reducibleBase * reductionPercent / 100.0);
                            event.currentCost = Math.max(0, event.currentCost - totalReduction);
                        }
                    }
                }
            }
        }
    }

}





