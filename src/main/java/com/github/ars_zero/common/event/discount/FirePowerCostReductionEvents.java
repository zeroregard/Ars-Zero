package com.github.ars_zero.common.event.discount;

import com.alexthw.sauce.registry.ModRegistry;
import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectIgnite;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.SubscribeEvent;

public class FirePowerCostReductionEvents extends AbstractConjureVoxelPowerCostReductionEvents {

    public static final FirePowerCostReductionEvents INSTANCE = new FirePowerCostReductionEvents();

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
        return part instanceof EffectIgnite;
    }

    @Override
    protected Holder<Attribute> getPowerAttribute() {
        return ModRegistry.FIRE_POWER;
    }
}
