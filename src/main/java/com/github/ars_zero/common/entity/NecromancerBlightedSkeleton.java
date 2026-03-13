package com.github.ars_zero.common.entity;

import com.github.ars_zero.common.entity.ai.FireVoxelPushSpellBehaviour;
import com.github.ars_zero.common.entity.ai.IceVoxelPushSpellBehaviour;
import com.github.ars_zero.common.entity.ai.MageSkeletonCastGoal;
import com.github.ars_zero.common.entity.ai.NecromancerRitualGoal;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Skeleton;
import com.github.ars_zero.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.List;

/**
 * Tier 2 blighted skeleton: ritual summoning, fire and ice voxel casts.
 */
public class NecromancerBlightedSkeleton extends AbstractBlightedSkeleton {

    private static final int MAX_MANA = 3000;
    private static final double MANA_REGEN = 2.0;

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
                new IceVoxelPushSpellBehaviour())));
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType) {
        // Require smooth_corrupted_sourcestone_small_bricks underfoot (instead of plain variant)
        BlockPos below = blockPosition().below();
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(level.getBlockState(below).getBlock());
        if (id == null || !id.getNamespace().equals(com.github.ars_zero.ArsZero.MOD_ID)
                || !id.getPath().equals("smooth_corrupted_sourcestone_small_bricks")) return false;
        // Also require an ossuary beacon within 16 blocks
        BlockPos origin = blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(
                origin.offset(-16, -4, -16),
                origin.offset(16, 4, 16))) {
            if (level.getBlockState(p).is(ModBlocks.OSSUARY_BEACON.get())) return true;
        }
        return false;
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
