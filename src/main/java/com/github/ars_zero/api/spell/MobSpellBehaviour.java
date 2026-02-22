package com.github.ars_zero.api.spell;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Reusable cast logic for mobs. Can be selected by chance later (e.g. 80% blight+push, 20% other).
 */
public interface MobSpellBehaviour {

    /**
     * Estimated mana cost for this behaviour (used to check if mob can cast before running).
     */
    int getManaCost();

    /**
     * Run the cast: spawn circle, spawn voxel, resolve Push, etc.
     * Caller should have already checked canCast (mana + cooldown) and that target is valid.
     *
     * @return true if the cast succeeded (mana was expended).
     */
    boolean run(Mob caster, LivingEntity target);
}
