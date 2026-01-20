package com.github.ars_zero.common.event.discount;

import com.github.ars_zero.common.glyph.ConjureVoxelEffect;
import com.github.ars_zero.common.util.SpellDiscountUtil;
import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;

public abstract class AbstractConjureVoxelPowerCostReductionEvents {
    
    protected abstract boolean isTargetEffect(AbstractSpellPart part);
    
    protected abstract Holder<Attribute> getPowerAttribute();
    
    protected void applyCostReduction(SpellCostCalcEvent event) {
        if (!(event.context.getCaster() instanceof LivingCaster caster)) {
            return;
        }
        if (!(caster.livingEntity instanceof Player player)) {
            return;
        }
        if (player instanceof FakePlayer) {
            return;
        }
        
        java.util.List<AbstractSpellPart> recipe = java.util.stream.StreamSupport.stream(event.context.getSpell().recipe().spliterator(), false).toList();
        AbstractSpellPart prev = null;
        int augmentEffectCost = 0;
        boolean foundPair = false;
        
        for (AbstractSpellPart part : recipe) {
            if (prev instanceof ConjureVoxelEffect) {
                if (isTargetEffect(part)) {
                    augmentEffectCost = part.getCastingCost();
                    foundPair = true;
                    break;
                }
            }
            prev = part;
        }
        
        if (foundPair && augmentEffectCost > 0) {
            event.currentCost = Math.max(0, event.currentCost - augmentEffectCost);
            
            Holder<Attribute> powerAttribute = getPowerAttribute();
            if (powerAttribute == null) {
                return;
            }
            AttributeInstance powerInstance = player.getAttribute(powerAttribute);
            if (powerInstance == null) {
                return;
            }
            double power = powerInstance.getValue();
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
