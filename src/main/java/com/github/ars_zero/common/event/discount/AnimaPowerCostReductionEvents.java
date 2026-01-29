package com.github.ars_zero.common.event.discount;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.common.glyph.EffectConjureBlight;
import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.SubscribeEvent;

public class AnimaPowerCostReductionEvents extends AbstractConjureVoxelPowerCostReductionEvents {
    
    public static final AnimaPowerCostReductionEvents INSTANCE = new AnimaPowerCostReductionEvents();
    
    @SubscribeEvent
    public static void onSpellCostCalcPre(SpellCostCalcEvent.Pre event) {
        INSTANCE.applyCostReduction(event);
    }
    
    @SubscribeEvent
    public static void onSpellCostCalcPost(SpellCostCalcEvent.Post event) {
        INSTANCE.applyCostReduction(event);
    }
    
    @Override
    protected boolean isTargetEffect(AbstractSpellPart part) {
        return part instanceof EffectConjureBlight;
    }
    
    @Override
    protected Holder<Attribute> getPowerAttribute() {
        return ModRegistry.NECROMANCY_POWER;
    }
}
