package com.github.ars_zero.common.entity;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import net.minecraft.world.entity.LivingEntity;

public interface ILifespanExtendable {
    void addLifespan(LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver);
}



