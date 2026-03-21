package com.github.ars_zero.common.entity;

import com.github.ars_zero.common.entity.ai.BlightVoxelPushSpellBehaviour;
import com.github.ars_zero.common.entity.ai.BlightedSkeletonFleeGoal;
import com.github.ars_zero.common.entity.ai.MageSkeletonCastGoal;
import com.github.ars_zero.common.entity.ai.NecromancerRitualGoal;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Tier 1 blighted skeleton: blight cast only, no blink, no summon; flees when low on mana.
 */
public class AcolyteBlightedSkeleton extends AbstractBlightedSkeleton {

    private static final int MAX_MANA = 600;
    private static final double MANA_REGEN = 0.3;

    public AcolyteBlightedSkeleton(EntityType<? extends Skeleton> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new BlightedSkeletonFleeGoal(this));
        this.goalSelector.addGoal(2, new NecromancerRitualGoal(this));
        this.goalSelector.addGoal(2, new MageSkeletonCastGoal(this, List.of(new BlightVoxelPushSpellBehaviour())));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setRottedArcanistSlot(EquipmentSlot.CHEST);
        setRottedArcanistSlot(EquipmentSlot.LEGS);
        setRottedArcanistSlot(EquipmentSlot.FEET);
    }

    @Override
    public int getMaxMana() {
        return MAX_MANA;
    }

    @Override
    public double getManaRegenPerTick() {
        return MANA_REGEN;
    }

    @Override
    public int getBlinkCooldownTicksMax() {
        return 0;
    }

    @Override
    public int getMaxSummons() {
        return 0;
    }

    @Override
    public boolean canFly() {
        return false;
    }

    @Override
    public boolean shouldFleeWhenLowMana() {
        return true;
    }
}
