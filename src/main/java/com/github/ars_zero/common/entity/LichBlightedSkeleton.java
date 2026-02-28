package com.github.ars_zero.common.entity;

import com.github.ars_zero.common.entity.ai.BlightVoxelPushSpellBehaviour;
import com.github.ars_zero.common.entity.ai.MageSkeletonBlinkGoal;
import com.github.ars_zero.common.entity.ai.MageSkeletonCastGoal;
import com.github.ars_zero.common.entity.ai.MageSkeletonSummonGoal;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tier 3 blighted skeleton: flying, unlimited blink, up to 2 summons, blight cast.
 */
public class LichBlightedSkeleton extends AbstractBlightedSkeleton {

    private static final int MAX_MANA = 3000;
    private static final double MANA_REGEN = 2.0;
    private static final int BLINK_COOLDOWN = 10;

    public LichBlightedSkeleton(EntityType<? extends Skeleton> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new MageSkeletonBlinkGoal(this));
        this.goalSelector.addGoal(1, new MageSkeletonSummonGoal(this));
        this.goalSelector.addGoal(2, new MageSkeletonCastGoal(this, List.of(new BlightVoxelPushSpellBehaviour())));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setFullArcanistGear();
        setSpellbookInHand();
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        setNoGravity(true);
        return result;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation flying = new FlyingPathNavigation(this, level);
        flying.setCanOpenDoors(false);
        flying.setCanFloat(true);
        flying.setCanPassDoors(true);
        return flying;
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
        return BLINK_COOLDOWN;
    }

    @Override
    public int getMaxSummons() {
        return 2;
    }

    @Override
    public boolean canFly() {
        return true;
    }

    @Override
    public boolean shouldFleeWhenLowMana() {
        return false;
    }
}
