package com.github.ars_zero.api.spell;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;

/**
 * Spell resolver for mob casters that respects mana: does not override {@link #enoughMana},
 * so the default implementation runs and {@link com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster#enoughMana(int)}
 * / {@link com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster#expendMana(int)} are used.
 * Use this with a mob that has {@link com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry#MANA_CAPABILITY} registered.
 */
public class MobSpellResolver extends SpellResolver {

    public MobSpellResolver(SpellContext context) {
        super(context);
    }
}
