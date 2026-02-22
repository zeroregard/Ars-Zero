package com.github.ars_zero.common.entity;

import com.github.ars_zero.ArsZero;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import com.github.ars_zero.common.entity.ai.BlightVoxelPushSpellBehaviour;
import com.github.ars_zero.common.entity.ai.MageSkeletonBlinkGoal;
import com.github.ars_zero.common.entity.ai.MageSkeletonCastGoal;
import com.github.ars_zero.common.entity.ai.MageSkeletonHoldPositionGoal;
import com.github.ars_zero.common.entity.ai.MageSkeletonSummonGoal;
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
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A skeleton variant that spawns wearing an Ars Nouveau arcanist hat and holding a spellbook.
 * No bow, no attack goals for now. Does not burn in sunlight.
 */
public class MageSkeletonEntity extends Skeleton {

    /** Ars Nouveau registry id for the arcanist hood (helmet). See LibItemNames.ARCANIST_HOOD. */
    private static final ResourceLocation ARCANIST_HOOD_ID =
            ResourceLocation.fromNamespaceAndPath("ars_nouveau", "arcanist_hood");
    /** Ars Nouveau novice spell book (held in main hand instead of bow). */
    private static final ResourceLocation NOVICE_SPELL_BOOK_ID =
            ResourceLocation.fromNamespaceAndPath("ars_nouveau", "novice_spell_book");

    private static final int MAX_MANA = 3000;
    private static final double MANA_REGEN_PER_TICK = 2.0;

    /** Ticks until next cast allowed; decremented every tick. */
    private int castCooldownTicks = 0;
    /** Countdown for hover before pushing (0 = not charging). */
    private int chargeTicks = 0;
    /** Entity id of voxel to push when chargeTicks hits 0. */
    private int pendingVoxelId = -1;
    /** Ticks until next blink allowed; decremented every tick. */
    private int blinkCooldownTicks = 0;
    /** Ticks until next summon allowed (avoids double-summon race); decremented every tick. */
    private int summonCooldownTicks = 0;
    /** UUID of the revenant we summoned, if any. Cleared when it dies or is missing. */
    @Nullable
    private UUID ownedRevenantUuid = null;

    public MageSkeletonEntity(EntityType<? extends Skeleton> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        // Do not call super: we want no melee or bow goals, only spell casting
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.goalSelector.addGoal(0, new MageSkeletonBlinkGoal(this));
        this.goalSelector.addGoal(1, new MageSkeletonSummonGoal(this));
        this.goalSelector.addGoal(2, new MageSkeletonCastGoal(this, List.of(new BlightVoxelPushSpellBehaviour())));
        this.goalSelector.addGoal(3, new MageSkeletonHoldPositionGoal(this));
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
            mana.setMaxMana(MAX_MANA);
            mana.setMana(MAX_MANA);
        }
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (castCooldownTicks > 0) {
                castCooldownTicks--;
            }
            if (blinkCooldownTicks > 0) {
                blinkCooldownTicks--;
            }
            if (summonCooldownTicks > 0) {
                summonCooldownTicks--;
            }
            if (ownedRevenantUuid != null && level() instanceof net.minecraft.server.level.ServerLevel sl) {
                Entity e = sl.getEntity(ownedRevenantUuid);
                if (e == null || !e.isAlive()) {
                    ownedRevenantUuid = null;
                }
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
                        BlightVoxelPushSpellBehaviour.executePush(this, getTarget(), voxel);
                    }
                    castCooldownTicks = MageSkeletonCastGoal.COOLDOWN_TICKS;
                }
            }
            IManaCap mana = CapabilityRegistry.getMana(this);
            if (mana != null && mana.getCurrentMana() < mana.getMaxMana()) {
                double newMana = Math.min(mana.getMaxMana(), mana.getCurrentMana() + MANA_REGEN_PER_TICK);
                mana.setMana(newMana);
            }
        }
    }

    public int getCastCooldownTicks() {
        return castCooldownTicks;
    }

    public void setCastCooldownTicks(int ticks) {
        this.castCooldownTicks = ticks;
    }

    public int getBlinkCooldownTicks() {
        return blinkCooldownTicks;
    }

    public void setBlinkCooldownTicks(int ticks) {
        this.blinkCooldownTicks = ticks;
    }

    public int getSummonCooldownTicks() {
        return summonCooldownTicks;
    }

    public void setSummonCooldownTicks(int ticks) {
        this.summonCooldownTicks = ticks;
    }

    /** UUID of the revenant this mage summoned; null if none or it died. */
    @Nullable
    public UUID getOwnedRevenantUuid() {
        return ownedRevenantUuid;
    }

    public void setOwnedRevenantUuid(@Nullable UUID uuid) {
        this.ownedRevenantUuid = uuid;
    }

    /** True if we currently have a living revenant we summoned (tracked by UUID). */
    public boolean hasLivingOwnedRevenant() {
        if (ownedRevenantUuid == null || !(level() instanceof net.minecraft.server.level.ServerLevel sl)) {
            return false;
        }
        Entity e = sl.getEntity(ownedRevenantUuid);
        return e != null && e.isAlive();
    }

    public int getChargeTicks() {
        return chargeTicks;
    }

    public void setPendingPush(int voxelEntityId, int hoverTicks) {
        this.pendingVoxelId = voxelEntityId;
        this.chargeTicks = hoverTicks;
    }

    private static final String TAG_OWNED_REVENANT = "OwnedRevenantUUID";

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownedRevenantUuid != null) {
            tag.putUUID(TAG_OWNED_REVENANT, ownedRevenantUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownedRevenantUuid = tag.hasUUID(TAG_OWNED_REVENANT) ? tag.getUUID(TAG_OWNED_REVENANT) : null;
    }

    private void setLookAtTarget(LivingEntity target) {
        Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(getEyePosition(1.0f)).normalize();
        double horizontalLength = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        setYRot((float) (Math.atan2(-toTarget.x, toTarget.z) * 180.0 / Math.PI));
        setXRot((float) (Math.atan2(-toTarget.y, horizontalLength) * 180.0 / Math.PI));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        // No bow; give spellbook in main hand and arcanist hood on head
        setArcanistHat();
        setSpellbookInHand();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.WITHER) || source.is(DamageTypes.WITHER_SKULL)) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof BlightVoxelEntity voxel && voxel.getStoredCaster() == this) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect().value() == MobEffects.WITHER) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    private void setArcanistHat() {
        Item hood = BuiltInRegistries.ITEM.get(ARCANIST_HOOD_ID);
        if (hood != null && hood != Items.AIR) {
            ItemStack stack = new ItemStack(hood);
            stack.set(DataComponents.BASE_COLOR, DyeColor.BLACK);
            setItemSlot(EquipmentSlot.HEAD, stack);
            setDropChance(EquipmentSlot.HEAD, 0.0f);
        } else {
            ArsZero.LOGGER.debug("Ars Nouveau arcanist hood not found at {}, mage skeleton will have no hat", ARCANIST_HOOD_ID);
        }
    }

    private void setSpellbookInHand() {
        Item spellbook = BuiltInRegistries.ITEM.get(NOVICE_SPELL_BOOK_ID);
        if (spellbook != null && spellbook != Items.AIR) {
            ItemStack stack = new ItemStack(spellbook);
            stack.set(DataComponents.BASE_COLOR, DyeColor.BLACK);
            setItemSlot(EquipmentSlot.MAINHAND, stack);
            setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        } else {
            ArsZero.LOGGER.debug("Ars Nouveau novice spell book not found at {}, mage skeleton will have empty hand", NOVICE_SPELL_BOOK_ID);
        }
    }
}
