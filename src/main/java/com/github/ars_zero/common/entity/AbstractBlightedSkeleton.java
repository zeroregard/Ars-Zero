package com.github.ars_zero.common.entity;

import com.github.ars_zero.ArsZero;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.core.component.DataComponents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Abstract base for blighted skeleton mages (Acolyte, Necromancer, Lich).
 * No bow, no melee; subclasses register blink, summon, and cast goals as appropriate.
 * Does not burn in sunlight. Tier behaviour is configured via abstract getters.
 */
public abstract class AbstractBlightedSkeleton extends Skeleton {

    protected static final ResourceLocation ARCANIST_HOOD_ID =
            ResourceLocation.fromNamespaceAndPath("ars_nouveau", "arcanist_hood");
    protected static final ResourceLocation SORCERER_HOOD_ID =
            ResourceLocation.fromNamespaceAndPath("ars_nouveau", "sorcerer_hood");
    protected static final ResourceLocation ARCANIST_ROBES_ID =
            ResourceLocation.fromNamespaceAndPath("ars_nouveau", "arcanist_robes");
    protected static final ResourceLocation ARCANIST_LEGGINGS_ID =
            ResourceLocation.fromNamespaceAndPath("ars_nouveau", "arcanist_leggings");
    protected static final ResourceLocation ARCANIST_BOOTS_ID =
            ResourceLocation.fromNamespaceAndPath("ars_nouveau", "arcanist_boots");
    protected static final ResourceLocation NOVICE_SPELL_BOOK_ID =
            ResourceLocation.fromNamespaceAndPath("ars_nouveau", "novice_spell_book");

    private int castCooldownTicks = 0;
    private int chargeTicks = 0;
    private int pendingVoxelId = -1;
    private int blinkCooldownTicks = 0;
    private int summonCooldownTicks = 0;
    private final List<UUID> ownedRevenantUuids = new ArrayList<>();

    protected AbstractBlightedSkeleton(EntityType<? extends Skeleton> entityType, Level level) {
        super(entityType, level);
    }

    /** Max mana pool for this tier. */
    public abstract int getMaxMana();

    /** Mana regenerated per tick. */
    public abstract double getManaRegenPerTick();

    /** Cooldown in ticks after blinking (0 = no blink / unlimited). */
    public abstract int getBlinkCooldownTicksMax();

    /** Max number of revenants this tier can have at once (0 = no summon). */
    public abstract int getMaxSummons();

    /** True for Lich (flying). */
    public abstract boolean canFly();

    /** True for Acolyte (flee when out of mana). */
    public abstract boolean shouldFleeWhenLowMana();

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.goalSelector.addGoal(2, new com.github.ars_zero.common.entity.ai.BlightedSkeletonMoveToTargetGoal(this));
        this.goalSelector.addGoal(3, new com.github.ars_zero.common.entity.ai.MageSkeletonHoldPositionGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, LivingEntity.class, 8.0F));
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        IManaCap mana = CapabilityRegistry.getMana(this);
        if (mana != null) {
            int max = getMaxMana();
            mana.setMaxMana(max);
            mana.setMana(max);
        }
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (castCooldownTicks > 0) castCooldownTicks--;
            if (blinkCooldownTicks > 0) blinkCooldownTicks--;
            if (summonCooldownTicks > 0) summonCooldownTicks--;
            if (level() instanceof net.minecraft.server.level.ServerLevel sl && !ownedRevenantUuids.isEmpty()) {
                ownedRevenantUuids.removeIf(uuid -> {
                    Entity e = sl.getEntity(uuid);
                    return e == null || !e.isAlive();
                });
            }
            if (chargeTicks > 0) {
                LivingEntity target = getTarget();
                if (target != null && target.isAlive()) {
                    setLookAtTarget(target);
                    Entity e = level().getEntity(pendingVoxelId);
                    if (e instanceof BlightVoxelEntity voxel && voxel.isAlive()) {
                        Vec3 inFront = getEyePosition(1.0f).add(getLookAngle().scale(1.0));
                        voxel.setPos(inFront.x, inFront.y, inFront.z);
                    }
                }
                chargeTicks--;
                if (chargeTicks == 0 && pendingVoxelId != -1) {
                    Entity e = level().getEntity(pendingVoxelId);
                    pendingVoxelId = -1;
                    if (e instanceof BlightVoxelEntity voxel && voxel.isAlive() && getTarget() != null) {
                        com.github.ars_zero.common.entity.ai.BlightVoxelPushSpellBehaviour.executePush(this, getTarget(), voxel);
                    }
                    castCooldownTicks = com.github.ars_zero.common.entity.ai.MageSkeletonCastGoal.COOLDOWN_TICKS;
                }
            }
            IManaCap mana = CapabilityRegistry.getMana(this);
            if (mana != null && mana.getCurrentMana() < mana.getMaxMana()) {
                double newMana = Math.min(mana.getMaxMana(), mana.getCurrentMana() + getManaRegenPerTick());
                mana.setMana(newMana);
            }
        }
    }

    public int getCastCooldownTicks() { return castCooldownTicks; }
    public void setCastCooldownTicks(int ticks) { this.castCooldownTicks = ticks; }
    public int getBlinkCooldownTicks() { return blinkCooldownTicks; }
    public void setBlinkCooldownTicks(int ticks) { this.blinkCooldownTicks = ticks; }
    public int getSummonCooldownTicks() { return summonCooldownTicks; }
    public void setSummonCooldownTicks(int ticks) { this.summonCooldownTicks = ticks; }
    public int getChargeTicks() { return chargeTicks; }
    public void setPendingPush(int voxelEntityId, int hoverTicks) {
        this.pendingVoxelId = voxelEntityId;
        this.chargeTicks = hoverTicks;
    }

    public int countLivingOwnedRevenants() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel sl)) return 0;
        int count = 0;
        for (UUID uuid : ownedRevenantUuids) {
            Entity e = sl.getEntity(uuid);
            if (e != null && e.isAlive()) count++;
        }
        return count;
    }

    public void addOwnedRevenantUuid(UUID uuid) {
        if (ownedRevenantUuids.size() < getMaxSummons()) {
            ownedRevenantUuids.add(uuid);
        }
    }

    private static final String TAG_OWNED_REVENANTS = "OwnedRevenantUUIDs";

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag list = new ListTag();
        for (UUID u : ownedRevenantUuids) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", u);
            list.add(entry);
        }
        tag.put(TAG_OWNED_REVENANTS, list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownedRevenantUuids.clear();
        if (tag.contains(TAG_OWNED_REVENANTS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(TAG_OWNED_REVENANTS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (entry.hasUUID("Id")) {
                    ownedRevenantUuids.add(entry.getUUID("Id"));
                }
            }
        }
    }

    protected void setLookAtTarget(LivingEntity target) {
        Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(getEyePosition(1.0f)).normalize();
        double horizontalLength = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        setYRot((float) (Math.atan2(-toTarget.x, toTarget.z) * 180.0 / Math.PI));
        setXRot((float) (Math.atan2(-toTarget.y, horizontalLength) * 180.0 / Math.PI));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setHeadGear();
        setSpellbookInHand();
    }

    /** Which head item this tier wears (arcanist hood by default; Acolyte uses sorcerer hood). */
    protected ResourceLocation getHeadItemId() {
        return ARCANIST_HOOD_ID;
    }

    /** Sets head slot from getHeadItemId(); subclasses override getHeadItemId() to change. */
    protected void setHeadGear() {
        Item hood = BuiltInRegistries.ITEM.get(getHeadItemId());
        if (hood != null && hood != Items.AIR) {
            ItemStack stack = new ItemStack(hood);
            stack.set(DataComponents.BASE_COLOR, DyeColor.BLACK);
            setItemSlot(EquipmentSlot.HEAD, stack);
            setDropChance(EquipmentSlot.HEAD, 0.0f);
        } else {
            ArsZero.LOGGER.debug("Ars Nouveau head gear not found at {}, blighted skeleton will have no hat", getHeadItemId());
        }
    }

    /** Full arcanist set (hood, robes, leggings, boots) in black; used by Lich. */
    protected void setFullArcanistGear() {
        setArmorSlot(EquipmentSlot.HEAD, ARCANIST_HOOD_ID);
        setArmorSlot(EquipmentSlot.CHEST, ARCANIST_ROBES_ID);
        setArmorSlot(EquipmentSlot.LEGS, ARCANIST_LEGGINGS_ID);
        setArmorSlot(EquipmentSlot.FEET, ARCANIST_BOOTS_ID);
    }

    private void setArmorSlot(EquipmentSlot slot, ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item != null && item != Items.AIR) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.BASE_COLOR, DyeColor.BLACK);
            setItemSlot(slot, stack);
            setDropChance(slot, 0.0f);
        } else {
            ArsZero.LOGGER.debug("Ars Nouveau armor not found at {}, slot {} will be empty", itemId, slot);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.WITHER) || source.is(DamageTypes.WITHER_SKULL)) return false;
        Entity direct = source.getDirectEntity();
        if (direct instanceof BlightVoxelEntity voxel && voxel.getStoredCaster() == this) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect().value() == MobEffects.WITHER) return false;
        return super.canBeAffected(effect);
    }

    @Override
    protected boolean isSunBurnTick() { return false; }

    protected void setSpellbookInHand() {
        Item spellbook = BuiltInRegistries.ITEM.get(NOVICE_SPELL_BOOK_ID);
        if (spellbook != null && spellbook != Items.AIR) {
            ItemStack stack = new ItemStack(spellbook);
            stack.set(DataComponents.BASE_COLOR, DyeColor.BLACK);
            setItemSlot(EquipmentSlot.MAINHAND, stack);
            setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        } else {
            ArsZero.LOGGER.debug("Ars Nouveau novice spell book not found at {}, blighted skeleton will have empty hand", NOVICE_SPELL_BOOK_ID);
        }
    }
}
