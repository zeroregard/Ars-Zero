package com.github.ars_zero.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Bone Golem: an undead hostile golem constructed from 4 bone blocks and a
 * skeleton (or wither skeleton) skull. Has slightly less health than an Iron
 * Golem and targets all living non-undead mobs.
 *
 * The model and texture intentionally reuse the Iron Golem's; a custom
 * appearance can be swapped in later.
 */
public class BoneGolem extends IronGolem {

    public BoneGolem(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
    }

    // -----------------------------------------------------------------------
    // Attributes
    // -----------------------------------------------------------------------

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                // Keep Iron Golem's default movement speed and attack damage.
                ;
    }

    // -----------------------------------------------------------------------
    // Undead classification
    // -----------------------------------------------------------------------

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    // -----------------------------------------------------------------------
    // AI – target players and other non-undead mobs
    // -----------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Target players (Iron Golem normally ignores players unless they
        // attacked a villager; override that by adding a player target goal).
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
}
