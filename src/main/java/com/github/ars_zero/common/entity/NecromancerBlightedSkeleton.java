package com.github.ars_zero.common.entity;

import com.github.ars_zero.common.entity.ai.BlightVoxelPushSpellBehaviour;
import com.github.ars_zero.common.entity.ai.FireVoxelPushSpellBehaviour;
import com.github.ars_zero.common.entity.ai.IceVoxelPushSpellBehaviour;
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
 * Tier 2 blighted skeleton: ritual summoning, fire and ice voxel casts.
 */
public class NecromancerBlightedSkeleton extends AbstractBlightedSkeleton {

    private static final int MAX_MANA = 800;
    private static final double MANA_REGEN = 1.0;

    public NecromancerBlightedSkeleton(EntityType<? extends Skeleton> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setTatteredArcanistSlot(EquipmentSlot.HEAD);
        setTatteredArcanistSlot(EquipmentSlot.CHEST);
        setTatteredArcanistSlot(EquipmentSlot.LEGS);
        setTatteredArcanistSlot(EquipmentSlot.FEET);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new NecromancerRitualGoal(this));
        this.goalSelector.addGoal(2, new MageSkeletonCastGoal(this, List.of(
                new FireVoxelPushSpellBehaviour(),
                new IceVoxelPushSpellBehaviour(),
                new BlightVoxelPushSpellBehaviour())));
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
        return false;
    }
}
