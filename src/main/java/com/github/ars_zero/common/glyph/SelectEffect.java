package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.common.spell.IMultiPhaseCaster;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.TemporalContextRecorder;
import com.github.ars_zero.common.util.BlockImmutabilityUtil;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtract;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SelectEffect extends AbstractEffect {
    
    public static final String ID = "select_effect";
    public static final SelectEffect INSTANCE = new SelectEffect();

    public SelectEffect() {
        super(ArsZero.prefix(ID), "Select");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;
        // Entity selection handled elsewhere
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide || !(shooter instanceof Player player) || !(world instanceof ServerLevel serverLevel)) return;
        if (spellStats.isSensitive()) return;

        List<BlockPos> validBlocks = computeValidBlocksFromHit(world, serverLevel, shooter, rayTraceResult, spellStats);
        if (validBlocks.isEmpty()) return;

        if (tryMergeChainingAndRecord(serverLevel, world, player, shooter, validBlocks, spellStats, spellContext)) return;

        FilteredBlocks filtered = getFilteredBlocks(serverLevel, validBlocks, spellContext, shooter);
        if (filtered.positions().isEmpty()) return;
        recordSelection(serverLevel, filtered, player, spellContext, shooter, spellStats.hasBuff(AugmentExtract.INSTANCE));
    }

    /** AOE from hit, then filter to valid selectable blocks. */
    private List<BlockPos> computeValidBlocksFromHit(Level world, ServerLevel serverLevel, LivingEntity shooter,
            BlockHitResult rayTraceResult, SpellStats spellStats) {
        BlockPos pos = rayTraceResult.getBlockPos();
        if (!BlockUtil.destroyRespectsClaim(getPlayer(shooter, serverLevel), world, pos)) return List.of();
        if (!BlockImmutabilityUtil.canBlockBeDestroyed(world, pos)) return List.of();

        List<BlockPos> posList = SpellUtil.calcAOEBlocks(shooter, pos, rayTraceResult,
                spellStats.getAoeMultiplier(), spellStats.getBuffCount(AugmentPierce.INSTANCE));
        List<BlockPos> valid = new ArrayList<>();
        for (BlockPos blockPos : posList) {
            if (world.isOutsideBuildHeight(blockPos)) continue;
            if (!BlockUtil.destroyRespectsClaim(getPlayer(shooter, serverLevel), world, blockPos)) continue;
            if (!BlockImmutabilityUtil.canBlockBeDestroyed(world, blockPos)) continue;
            if (!BlockImmutabilityUtil.isPistonPushable(world.getBlockState(blockPos))) continue;
            valid.add(blockPos);
        }
        return valid;
    }

    /** Chaining + Select: merge existing block results with current hit into one group and record. Returns true if handled. */
    private boolean tryMergeChainingAndRecord(ServerLevel serverLevel, Level world, Player player, LivingEntity shooter,
            List<BlockPos> validBlocks, SpellStats spellStats, SpellContext spellContext) {
        // With Extract we want one result per block so Temporal Context runs the continuation per block; do not merge.
        if (spellStats.hasBuff(AugmentExtract.INSTANCE)) return false;

        IMultiPhaseCaster caster = IMultiPhaseCaster.from(spellContext, shooter);
        MultiPhaseCastContext context = caster != null ? caster.getCastContext() : null;
        if (context == null || context.beginResults.isEmpty()) return false;
        if (!context.beginResults.stream().allMatch(r -> r != null && isBlockResult(r))) return false;

        List<BlockPos> merged = new ArrayList<>(new LinkedHashSet<>(collectBlockPositionsFromResults(context.beginResults)));
        merged.addAll(validBlocks);
        merged = new ArrayList<>(new LinkedHashSet<>(merged));

        FilteredBlocks validMerged = filterAndValidateBlockPositions(serverLevel, shooter, world, merged);
        if (validMerged.positions().isEmpty()) return true;

        // Discard any BlockGroupEntities from previous sub-resolutions so only the merged entity remains and removes blocks.
        for (SpellResult r : context.beginResults) {
            if (r != null && r.blockGroup != null && r.blockGroup.isAlive()) {
                r.blockGroup.discard();
            }
        }
        context.beginResults.clear();
        recordSelection(serverLevel, validMerged, player, spellContext, shooter, false);
        return true;
    }

    /** Filter positions to in-world, claimable, destroyable, piston-pushable; return positions + states. */
    private FilteredBlocks filterAndValidateBlockPositions(ServerLevel serverLevel, LivingEntity shooter, Level world, List<BlockPos> positions) {
        List<BlockPos> valid = new ArrayList<>();
        Map<BlockPos, BlockState> states = new java.util.HashMap<>();
        for (BlockPos p : positions) {
            if (serverLevel.isOutsideBuildHeight(p)) continue;
            if (!BlockUtil.destroyRespectsClaim(getPlayer(shooter, serverLevel), world, p)) continue;
            if (!BlockImmutabilityUtil.canBlockBeDestroyed(world, p)) continue;
            BlockState state = serverLevel.getBlockState(p);
            if (state.isAir() || BlockImmutabilityUtil.isBlockImmutable(state) || !BlockImmutabilityUtil.isPistonPushable(state)) continue;
            valid.add(p);
            states.put(p, state);
        }
        return new FilteredBlocks(valid, states);
    }

    private void recordSelection(ServerLevel serverLevel, FilteredBlocks filtered, Player player, SpellContext spellContext,
            LivingEntity shooter, boolean useExtract) {
        if (useExtract) {
            TemporalContextRecorder.recordBlockPositionsOnly(spellContext, filtered.positions());
        } else {
            createBlockGroup(serverLevel, filtered.positions(), filtered.states(), player, spellContext, shooter);
        }
    }

    private static boolean isBlockResult(SpellResult r) {
        return (r.blockPositions != null && !r.blockPositions.isEmpty()) || r.targetPosition != null || r.blockGroup != null;
    }

    private static List<BlockPos> collectBlockPositionsFromResults(List<SpellResult> results) {
        List<BlockPos> out = new ArrayList<>();
        for (SpellResult r : results) {
            if (r == null) continue;
            if (r.blockPositions != null) {
                out.addAll(r.blockPositions);
            } else if (r.targetPosition != null) {
                out.add(r.targetPosition);
            }
        }
        return out;
    }

    private record FilteredBlocks(List<BlockPos> positions, Map<BlockPos, BlockState> states) {}

    private FilteredBlocks getFilteredBlocks(ServerLevel level, List<BlockPos> blockPositions, SpellContext spellContext, LivingEntity shooter) {
        Map<BlockPos, BlockState> capturedStates = new java.util.HashMap<>();
        for (BlockPos pos : blockPositions) {
            if (!level.isOutsideBuildHeight(pos)) {
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && !BlockImmutabilityUtil.isBlockImmutable(state) && BlockImmutabilityUtil.isPistonPushable(state)) {
                    capturedStates.put(pos, state);
                }
            }
        }
        List<BlockPos> validPositions = new ArrayList<>(capturedStates.keySet());

        IMultiPhaseCaster caster = IMultiPhaseCaster.from(spellContext, shooter);
        MultiPhaseCastContext context = caster != null ? caster.getCastContext() : null;

        List<BlockPos> filteredPositions = validPositions;
        if (context != null) {
            Set<BlockPos> claimedBlocks = new java.util.HashSet<>();
            for (SpellResult result : context.beginResults) {
                if (result != null && result.blockGroup != null && result.blockPositions != null) {
                    claimedBlocks.addAll(result.blockPositions);
                }
            }
            filteredPositions = validPositions.stream()
                .filter(pos -> !claimedBlocks.contains(pos))
                .toList();
        }

        Map<BlockPos, BlockState> filteredStates = new java.util.HashMap<>();
        for (BlockPos pos : filteredPositions) {
            BlockState state = capturedStates.get(pos);
            if (state != null) {
                filteredStates.put(pos, state);
            }
        }
        return new FilteredBlocks(filteredPositions, filteredStates);
    }

    private void createBlockGroup(ServerLevel level, List<BlockPos> filteredPositions, Map<BlockPos, BlockState> capturedStates, Player player, SpellContext spellContext, LivingEntity shooter) {
        if (filteredPositions.isEmpty()) {
            return;
        }
        Vec3 centerPos = calculateCenter(filteredPositions);

        BlockGroupEntity blockGroup = new BlockGroupEntity(ModEntities.BLOCK_GROUP.get(), level);
        blockGroup.setPos(centerPos.x, centerPos.y, centerPos.z);
        blockGroup.setCasterUUID(player.getUUID());

        blockGroup.addBlocksWithStates(filteredPositions, capturedStates);

        level.addFreshEntity(blockGroup);

        TemporalContextRecorder.record(spellContext, blockGroup, filteredPositions);
    }
    
    
    private Vec3 calculateCenter(List<BlockPos> positions) {
        if (positions.isEmpty()) return Vec3.ZERO;
        
        double x = 0, y = 0, z = 0;
        for (BlockPos pos : positions) {
            x += pos.getX() + 0.5;
            y += pos.getY() + 0.5;
            z += pos.getZ() + 0.5;
        }
        
        int count = positions.size();
        return new Vec3(x / count, y / count, z / count);
    }

    @Override
    public int getDefaultManaCost() {
        return 0;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(
            AugmentAOE.INSTANCE,
            AugmentExtract.INSTANCE,
            AugmentPierce.INSTANCE,
            AugmentSensitive.INSTANCE
        );
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAOE.INSTANCE, "Increases the area of blocks that can be selected");
        map.put(AugmentExtract.INSTANCE, "Propagates block positions to later phases without creating a block group entity.");
        map.put(AugmentPierce.INSTANCE, "Increases the depth of blocks that can be selected");
        map.put(AugmentSensitive.INSTANCE, "Only selects entities, ignoring blocks.");
    }

    @Override
    public String getBookDescription() {
        return "Selects a target entity or block without performing any action. Use this to choose targets for future operations. AOE and Pierce allow selecting multiple blocks. For block translation, selected blocks are converted to a block group entity. Extract propagates block positions without creating a group. Sensitive restricts selection to entities only.";
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.ONE;
    }

    @NotNull
    @Override
    public Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.MANIPULATION);
    }

    @Override
    protected void addDefaultAugmentLimits(Map<ResourceLocation, Integer> defaults) {
        defaults.put(AugmentSensitive.INSTANCE.getRegistryName(), 1);
    }
}

