package com.github.ars_zero.common.event.discount;

import alexthw.ars_elemental.common.glyphs.EffectDischarge;
import com.alexthw.sauce.registry.ModRegistry;
import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectWindshear;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.SubscribeEvent;

public class AirPowerCostReductionEvents extends AbstractConjureVoxelPowerCostReductionEvents {
    
    public static final AirPowerCostReductionEvents INSTANCE = new AirPowerCostReductionEvents();
    
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
        return part instanceof EffectWindshear || part instanceof EffectDischarge;
    }
    
    @Override
    protected Holder<Attribute> getPowerAttribute() {
        return ModRegistry.AIR_POWER;
    }
}
