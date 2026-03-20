package com.github.ars_zero.common.entity;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ai.BlightVoxelPushSpellBehaviour;
import com.github.ars_zero.common.entity.ai.FireVoxelPushSpellBehaviour;
import com.github.ars_zero.common.entity.ai.IceVoxelPushSpellBehaviour;
import com.github.ars_zero.common.entity.ai.LightningVoxelPushSpellBehaviour;
import com.github.ars_zero.common.entity.ai.MageSkeletonBlinkGoal;
import com.github.ars_zero.common.entity.ai.MageSkeletonCastGoal;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import com.github.ars_zero.registry.ModItems;
import com.github.ars_zero.registry.ModSounds;

import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tier 3 blighted skeleton: flying, unlimited blink, fire/ice/lightning triple-voxel casts.
 */
public class LichBlightedSkeleton extends AbstractBlightedSkeleton {

    private static final TagKey<Item> LICH_STAVES_TAG = TagKey.create(
            Registries.ITEM, ArsZero.prefix("lich_staves"));

    private static final int MAX_MANA = 1500;
    private static final double MANA_REGEN = 2.0;
    private static final int BLINK_COOLDOWN = 200;
    /** Fake gravity per tick so the Lich slowly drifts down when in the air. */
    private static final double FAKE_GRAVITY_PER_TICK = -0.012;
    private static final double MAX_FALL_SPEED = -0.06;
    /** Ticks between each point of health regenerated. */
    private static final int REGEN_INTERVAL_TICKS = 40;
    private static final float REGEN_AMOUNT = 1.0f;

    public LichBlightedSkeleton(EntityType<? extends Skeleton> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FALL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new MageSkeletonBlinkGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(3, new MageSkeletonCastGoal(this, List.of(
                new BlightVoxelPushSpellBehaviour(),
                new FireVoxelPushSpellBehaviour(),
                new IceVoxelPushSpellBehaviour(),
                new LightningVoxelPushSpellBehaviour())));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.NECRO_CROWN.get()));
        setDropChance(EquipmentSlot.HEAD, 0.0f);
        setTatteredArcanistSlot(EquipmentSlot.CHEST);
        setTatteredArcanistSlot(EquipmentSlot.LEGS);
        setTatteredArcanistSlot(EquipmentSlot.FEET);
        level().registryAccess().lookup(Registries.ITEM).ifPresent(reg ->
                reg.get(LICH_STAVES_TAG).ifPresent(tag -> {
                    var list = tag.stream().toList();
                    if (!list.isEmpty()) {
                        Item staff = list.get(level().getRandom().nextInt(list.size())).value();
                        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(staff));
                        setDropChance(EquipmentSlot.MAINHAND, 1.0f);
                    }
                })
        );
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return target.hurt(this.damageSources().mobAttack(this), damage);
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
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (!onGround() && !isInWaterOrBubble()) {
            Vec3 motion = getDeltaMovement();
            double newY = Mth.clamp(motion.y + FAKE_GRAVITY_PER_TICK, MAX_FALL_SPEED, 1.0);
            setDeltaMovement(motion.x, newY, motion.z);
        }
        if (tickCount % REGEN_INTERVAL_TICKS == 0 && getHealth() < getMaxHealth()) {
            heal(REGEN_AMOUNT);
        }
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
        return 0;
    }

    @Override
    public boolean canFly() {
        return true;
    }

    @Override
    public boolean shouldFleeWhenLowMana() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() { return ModSounds.LICH_AMBIENT.get(); }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.LICH_HURT.get(); }

    @Override
    protected SoundEvent getDeathSound() { return ModSounds.LICH_DEATH.get(); }

    @Override
    protected SoundEvent getStepSound() { return ModSounds.LICH_STEP.get(); }
}
