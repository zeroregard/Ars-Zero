package com.github.ars_zero.common.event.discount;

import com.alexthw.sauce.registry.ModRegistry;
import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectColdSnap;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.SubscribeEvent;

public class WaterPowerCostReductionEvents extends AbstractConjureVoxelPowerCostReductionEvents {

    public static final WaterPowerCostReductionEvents INSTANCE = new WaterPowerCostReductionEvents();

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
        return part instanceof EffectConjureWater || part instanceof EffectColdSnap;
    }

    @Override
    protected Holder<Attribute> getPowerAttribute() {
        return ModRegistry.WATER_POWER;
    }
}
