package com.github.ars_zero.common.entity.mageblock;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.common.entity.AbstractGeometryProcessEntity;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.registry.ParticleTimelineRegistry;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.common.block.MageBlock;
import com.hollingsworth.arsnouveau.common.block.tile.MageBlockTile;
import com.hollingsworth.arsnouveau.common.items.curios.ShapersFocus;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;

public class GeometryMageBlockEntity extends AbstractGeometryProcessEntity {

    private static final EntityDataAccessor<Boolean> DATA_WAITING_FOR_MANA = SynchedEntityData
            .defineId(GeometryMageBlockEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_COLOR_R = SynchedEntityData
            .defineId(GeometryMageBlockEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_COLOR_G = SynchedEntityData
            .defineId(GeometryMageBlockEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_COLOR_B = SynchedEntityData
            .defineId(GeometryMageBlockEntity.class, EntityDataSerializers.FLOAT);

    private static final double BASE_MANA_COST_PER_BLOCK = 0.3;

    private float conjurationPower = 0.0f;
    private int augmentCount = 0;
    private boolean waitingForMana = false;
    private boolean mageBlockPermanent = false;
    private double mageBlockDurationMultiplier = 1.0;
    @Nullable
    private SpellContext mageBlockSpellContext = null;

    public GeometryMageBlockEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void setCaster(@Nullable LivingEntity caster) {
        super.setCaster(caster);
        if (caster != null) {
            this.conjurationPower = getConjurationPower(caster);
        }
    }

    private float getConjurationPower(LivingEntity caster) {
        if (caster instanceof Player player) {
            AttributeInstance instance = player.getAttribute(ModRegistry.SUMMON_POWER);
            if (instance != null) {
                return (float) instance.getValue();
            }
        }
        return 0.0f;
    }

    public void setAugmentCount(int count) {
        this.augmentCount = count;
    }

    public int getAugmentCount() {
        return this.augmentCount;
    }

    public void setMageBlockProperties(boolean isPermanent, double durationMultiplier, @Nullable SpellContext spellContext) {
        this.mageBlockPermanent = isPermanent;
        this.mageBlockDurationMultiplier = durationMultiplier;
        this.mageBlockSpellContext = spellContext;
        
        float r = 1.0f, g = 1.0f, b = 1.0f;
        if (spellContext != null) {
            var timeline = spellContext.getParticleTimeline(
                    ParticleTimelineRegistry.MAGEBLOCK_TIMELINE.get());
            ParticleColor color = timeline.getColor();
            if (color != null) {
                r = color.getRed();
                g = color.getGreen();
                b = color.getBlue();
            }
        }
        
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_COLOR_R, r);
            this.entityData.set(DATA_COLOR_G, g);
            this.entityData.set(DATA_COLOR_B, b);
        }
    }
    
    public float getColorR() {
        return this.entityData.get(DATA_COLOR_R);
    }
    
    public float getColorG() {
        return this.entityData.get(DATA_COLOR_G);
    }
    
    public float getColorB() {
        return this.entityData.get(DATA_COLOR_B);
    }

    public boolean isWaitingForMana() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_WAITING_FOR_MANA);
        }
        return this.waitingForMana;
    }

    private void setWaitingForMana(boolean waiting) {
        this.waitingForMana = waiting;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_WAITING_FOR_MANA, waiting);
        }
    }

    private double getManaCostPerBlock() {
        return BASE_MANA_COST_PER_BLOCK * (this.augmentCount + 1);
    }

    @Override
    protected float getBlocksPerTick() {
        return BASE_BLOCKS_PER_TICK * (1.0f + conjurationPower / 2.0f);
    }

    @Override
    protected void tickProcess() {
        if (!(this.level() instanceof ServerLevel serverLevel))
            return;

        updateTargetBlock();

        if (this.processIndex >= this.processQueue.size()) {
            this.discard();
            return;
        }

        if (this.paused) {
            setWaitingForMana(false);
            return;
        }

        Player claimActor = getClaimActor(serverLevel);
        if (claimActor == null) {
            setWaitingForMana(false);
            return;
        }

        double costPerBlock = getManaCostPerBlock();
        IManaCap manaCap = CapabilityRegistry.getMana(claimActor);
        if (manaCap == null) {
            setWaitingForMana(true);
            return;
        }

        float rate = getBlocksPerTick();
        this.blockAccumulator += rate;

        int blocksToPlace = (int) this.blockAccumulator;
        if (blocksToPlace <= 0) {
            setWaitingForMana(false);
            return;
        }

        double availableMana = manaCap.getCurrentMana();
        int affordableBlocks = (int) Math.floor(availableMana / costPerBlock);
        int blocksToPlaceThisTick = Math.min(blocksToPlace, Math.max(0, affordableBlocks));

        if (blocksToPlaceThisTick <= 0) {
            setWaitingForMana(true);
            return;
        }

        setWaitingForMana(false);

        BlockState mageBlockState = BlockRegistry.MAGE_BLOCK.get().defaultBlockState()
                .setValue(MageBlock.TEMPORARY, !mageBlockPermanent);

        int oldIndex = this.processIndex;
        this.processIndex = placeMageBlocks(serverLevel, this.processQueue, this.processIndex,
                blocksToPlaceThisTick, mageBlockState, claimActor);

        int blocksPlaced = this.processIndex - oldIndex;

        if (blocksPlaced > 0) {
            playProcessSound(serverLevel, this.processQueue.get(oldIndex), blocksPlaced);

            if (spellContext != null && spellResolver != null) {
                for (int i = oldIndex; i < this.processIndex && i < this.processQueue.size(); i++) {
                    BlockPos placedPos = this.processQueue.get(i);
                    ShapersFocus.tryPropagateBlockSpell(
                            new BlockHitResult(Vec3.atCenterOf(placedPos), Direction.UP, placedPos, false),
                            serverLevel, claimActor, spellContext, spellResolver);
                }
            }
        }

        for (int i = 0; i < blocksPlaced; i++) {
            if (!consumeManaForBlock(claimActor, costPerBlock)) {
                setWaitingForMana(true);
                this.blockAccumulator -= i;
                return;
            }
        }

        this.blockAccumulator -= blocksPlaced;
    }

    @Override
    protected ProcessResult processBlock(ServerLevel level, BlockPos pos) {
        return ProcessResult.SKIPPED;
    }

    @Override
    protected SoundEvent getProcessSound(SoundType soundType) {
        return soundType.getPlaceSound();
    }

    private int placeMageBlocks(ServerLevel level, java.util.List<BlockPos> queue, int startIndex, int blocksPerTick,
                                 BlockState stateToPlace, @Nullable Player claimActor) {
        if (queue == null || queue.isEmpty()) {
            return startIndex;
        }

        int index = Math.max(0, startIndex);
        int placed = 0;
        int budget = Math.max(1, blocksPerTick);

        while (placed < budget && index < queue.size()) {
            BlockPos target = queue.get(index);
            index++;

            if (!level.isLoaded(target) || level.isOutsideBuildHeight(target)) {
                continue;
            }

            BlockState existing = level.getBlockState(target);
            if (!existing.canBeReplaced()) {
                continue;
            }

            if (!com.github.ars_zero.common.util.BlockProtectionUtil.canBlockBePlaced(level, target, stateToPlace, claimActor)) {
                continue;
            }

            level.setBlock(target, stateToPlace, 3);
            
            if (level.getBlockEntity(target) instanceof MageBlockTile tile) {
                if (mageBlockSpellContext != null) {
                    var timeline = mageBlockSpellContext.getParticleTimeline(
                            ParticleTimelineRegistry.MAGEBLOCK_TIMELINE.get());
                    ParticleColor color = timeline.getColor();
                    if (color != null) {
                        tile.setColor(color);
                    }
                }
                tile.lengthModifier = mageBlockDurationMultiplier;
                tile.isPermanent = mageBlockPermanent;
                tile.updateBlock();
            }
            
            placed++;
        }

        return index;
    }

    @Nullable
    private Player getClaimActor(ServerLevel level) {
        UUID caster = getCasterUUID();
        if (caster == null)
            return null;
        if (level.getServer() == null || level.getServer().getPlayerList() == null)
            return null;
        return level.getServer().getPlayerList().getPlayer(caster);
    }

    private boolean consumeManaForBlock(Player player, double costPerBlock) {
        IManaCap manaCap = CapabilityRegistry.getMana(player);
        if (manaCap != null && manaCap.getCurrentMana() >= costPerBlock) {
            manaCap.removeMana(costPerBlock);
            return true;
        }
        return false;
    }

    @Override
    protected void onPauseToggled(boolean paused) {
        if (!paused) {
            setWaitingForMana(false);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WAITING_FOR_MANA, false);
        builder.define(DATA_COLOR_R, 1.0f);
        builder.define(DATA_COLOR_G, 1.0f);
        builder.define(DATA_COLOR_B, 1.0f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("waiting_for_mana")) {
            this.waitingForMana = compound.getBoolean("waiting_for_mana");
            this.entityData.set(DATA_WAITING_FOR_MANA, this.waitingForMana);
        }
        if (compound.contains("augment_count")) {
            this.augmentCount = compound.getInt("augment_count");
        }
        if (compound.contains("mage_block_permanent")) {
            this.mageBlockPermanent = compound.getBoolean("mage_block_permanent");
        }
        if (compound.contains("mage_block_duration_multiplier")) {
            this.mageBlockDurationMultiplier = compound.getDouble("mage_block_duration_multiplier");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("waiting_for_mana", this.waitingForMana);
        compound.putInt("augment_count", this.augmentCount);
        compound.putBoolean("mage_block_permanent", this.mageBlockPermanent);
        compound.putDouble("mage_block_duration_multiplier", this.mageBlockDurationMultiplier);
    }
}

