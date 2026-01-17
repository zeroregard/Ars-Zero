package com.github.ars_zero.common.entity.terrain;

import com.alexthw.sauce.registry.ModRegistry;
import com.github.ars_zero.common.entity.AbstractGeometryProcessEntity;
import com.github.ars_zero.common.entity.IManaDrainable;
import com.github.ars_zero.common.structure.ConvergenceStructureHelper;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.registry.ParticleTimelineRegistry;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.common.block.tile.MageBlockTile;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.hollingsworth.arsnouveau.common.items.curios.ShapersFocus;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;

public class GeometryTerrainEntity extends AbstractGeometryProcessEntity implements IManaDrainable {

    private static final EntityDataAccessor<Boolean> DATA_WAITING_FOR_MANA = SynchedEntityData
            .defineId(GeometryTerrainEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double BASE_MANA_COST_PER_BLOCK = 0.3;

    private float earthPower = 0.0f;
    private double accumulatedDrain = 0.0;
    private int ticksSinceLastDrainSync = 0;
    @Nullable
    private Player casterPlayer = null;
    @Nullable
    private BlockState terrainBlockState = null;
    private int augmentCount = 0;
    private boolean waitingForMana = false;

    private boolean isMageBlock = false;
    private boolean mageBlockPermanent = false;
    private double mageBlockDurationMultiplier = 1.0;
    @Nullable
    private SpellContext mageBlockSpellContext = null;

    public GeometryTerrainEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void setCaster(@Nullable LivingEntity caster) {
        super.setCaster(caster);
        if (caster instanceof Player player) {
            this.casterPlayer = player;
            this.earthPower = getEarthPower(caster);
        } else {
            this.casterPlayer = null;
        }
    }

    private float getEarthPower(LivingEntity caster) {
        if (caster instanceof Player player) {
            AttributeInstance instance = player.getAttribute(ModRegistry.EARTH_POWER);
            if (instance != null) {
                return (float) instance.getValue();
            }
        }
        return 0.0f;
    }

    public void setTerrainBlockState(BlockState blockState) {
        this.terrainBlockState = blockState;
    }

    public BlockState getTerrainBlockState() {
        return this.terrainBlockState != null ? this.terrainBlockState : Blocks.DIRT.defaultBlockState();
    }

    public void setAugmentCount(int count) {
        this.augmentCount = count;
    }

    public int getAugmentCount() {
        return this.augmentCount;
    }

    public void setMageBlockProperties(boolean isPermanent, double durationMultiplier,
            @Nullable SpellContext spellContext) {
        this.isMageBlock = true;
        this.mageBlockPermanent = isPermanent;
        this.mageBlockDurationMultiplier = durationMultiplier;
        this.mageBlockSpellContext = spellContext;
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

    private int getBlockTypeFactor() {
        BlockState block = getTerrainBlockState();
        Block b = block.getBlock();
        if (b == Blocks.DIRT || b == Blocks.COARSE_DIRT || b == Blocks.PODZOL
                || b == Blocks.GRASS_BLOCK || b == Blocks.GRAVEL || b == Blocks.MUD) {
            return 1;
        } else if (b == Blocks.COBBLESTONE || b == Blocks.COBBLED_DEEPSLATE
                || b == Blocks.SAND || b == Blocks.RED_SAND
                || b == Blocks.SANDSTONE || b == Blocks.RED_SANDSTONE) {
            return 2;
        }
        return 3;
    }

    @Override
    protected float getBlocksPerTick() {
        return BASE_BLOCKS_PER_TICK * (1.0f + earthPower / 2.0f);
    }

    @Override
    public double getManaCostPerBlock() {
        return calculateManaCostPerBlock();
    }

    @Override
    public double getAccumulatedDrain() {
        return accumulatedDrain;
    }

    @Override
    public void setAccumulatedDrain(double value) {
        this.accumulatedDrain = value;
    }

    @Override
    public int getTicksSinceLastDrainSync() {
        return ticksSinceLastDrainSync;
    }

    @Override
    public void setTicksSinceLastDrainSync(int value) {
        this.ticksSinceLastDrainSync = value;
    }

    @Override
    public Player getCasterPlayer() {
        return casterPlayer;
    }

    @Override
    public void setCasterPlayer(Player player) {
        this.casterPlayer = player;
    }

    private double calculateManaCostPerBlock() {
        return BASE_MANA_COST_PER_BLOCK * getBlockTypeFactor() * (this.augmentCount + 1);
    }

    @Override
    protected void tickProcess() {
        if (!(this.level() instanceof ServerLevel serverLevel))
            return;

        tickAndSyncDrain(serverLevel);

        updateTargetBlock();

        if (this.processIndex >= this.processQueue.size())

        {
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

        double costPerBlock = calculateManaCostPerBlock();
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

        int oldIndex = this.processIndex;

        if (isMageBlock) {
            this.processIndex = placeMageBlocks(serverLevel, this.processQueue, this.processIndex,
                    blocksToPlaceThisTick, getTerrainBlockState(), claimActor);
        } else {
            this.processIndex = ConvergenceStructureHelper.placeNext(serverLevel, this.processQueue, this.processIndex,
                    blocksToPlaceThisTick, getTerrainBlockState(), claimActor);
        }

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
            if (!consumeManaAndAccumulate(serverLevel, costPerBlock)) {
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

            if (!com.github.ars_zero.common.util.BlockProtectionUtil.canBlockBePlaced(level, target, stateToPlace,
                    claimActor)) {
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
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("waiting_for_mana")) {
            this.waitingForMana = compound.getBoolean("waiting_for_mana");
            this.entityData.set(DATA_WAITING_FOR_MANA, this.waitingForMana);
        }
        if (compound.contains("terrain_block")) {
            ResourceLocation blockId = ResourceLocation.parse(compound.getString("terrain_block"));
            Block block = BuiltInRegistries.BLOCK.get(blockId);
            if (block != null) {
                this.terrainBlockState = block.defaultBlockState();
            }
        }
        if (compound.contains("earth_power")) {
            this.earthPower = compound.getFloat("earth_power");
        }
        if (compound.contains("augment_count")) {
            this.augmentCount = compound.getInt("augment_count");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("waiting_for_mana", this.waitingForMana);
        if (this.terrainBlockState != null) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(this.terrainBlockState.getBlock());
            if (blockId != null) {
                compound.putString("terrain_block", blockId.toString());
            }
        }
        compound.putFloat("earth_power", this.earthPower);
        compound.putInt("augment_count", this.augmentCount);
    }
}
