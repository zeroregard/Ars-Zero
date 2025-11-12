package com.github.ars_zero.common.event;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.glyph.ConjureVoxelEffect;
import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;

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
                WaterVoxelGlyphs glyphs = getWaterVoxelGlyphs(event.context.getSpell().recipe());
                if (glyphs != null) {
                    AttributeInstance waterPower = player.getAttribute(ModRegistry.WATER_POWER);
                    if (waterPower != null) {
                        double power = waterPower.getValue();
                        if (power > 0) {
                            double reductionPercent;
                            if (power >= 4) {
                                reductionPercent = Math.min(85.0 + (power - 4) * 5.0, 95.0);
                            } else if (power >= 3) {
                                reductionPercent = 75.0;
                            } else if (power >= 2) {
                                reductionPercent = 60.0;
                            } else {
                                reductionPercent = 40.0;
                            }
                            
                            int totalReduction = (int) Math.ceil(event.currentCost * reductionPercent / 100.0);
                            event.currentCost -= totalReduction;
                            event.currentCost = Math.max(0, event.currentCost);
                        }
                    }
                }
            }
        }
    }

    private static WaterVoxelGlyphs getWaterVoxelGlyphs(Iterable<AbstractSpellPart> recipe) {
        ConjureVoxelEffect voxelGlyph = null;
        EffectConjureWater waterGlyph = null;
        
        for (AbstractSpellPart part : recipe) {
            if (part instanceof ConjureVoxelEffect) {
                voxelGlyph = (ConjureVoxelEffect) part;
            }
            if (part instanceof EffectConjureWater) {
                waterGlyph = (EffectConjureWater) part;
            }
        }
        
        return (voxelGlyph != null && waterGlyph != null) ? new WaterVoxelGlyphs(voxelGlyph, waterGlyph) : null;
    }
    
    private static class WaterVoxelGlyphs {
        final ConjureVoxelEffect voxelGlyph;
        final EffectConjureWater waterGlyph;
        
        WaterVoxelGlyphs(ConjureVoxelEffect voxelGlyph, EffectConjureWater waterGlyph) {
            this.voxelGlyph = voxelGlyph;
            this.waterGlyph = waterGlyph;
        }
    }
}


