package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
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
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
        if (world.isClientSide) return;
        if (!(shooter instanceof Player player)) return;
        if (!(world instanceof ServerLevel serverLevel)) return;
        
        if (spellStats.isSensitive()) {
            return;
        }
        
        BlockPos pos = rayTraceResult.getBlockPos();
        if (!BlockUtil.destroyRespectsClaim(getPlayer(shooter, serverLevel), world, pos)) {
            return;
        }
        
        if (!BlockImmutabilityUtil.canBlockBeDestroyed(world, pos)) {
            return;
        }
        double aoeBuff = spellStats.getAoeMultiplier();
        int pierceBuff = spellStats.getBuffCount(AugmentPierce.INSTANCE);
        List<BlockPos> posList = SpellUtil.calcAOEBlocks(shooter, pos, rayTraceResult, aoeBuff, pierceBuff);
        
        List<BlockPos> validBlocks = new ArrayList<>();
        for (BlockPos blockPos : posList) {
            if (!world.isOutsideBuildHeight(blockPos) 
                && BlockUtil.destroyRespectsClaim(getPlayer(shooter, serverLevel), world, blockPos)
                && BlockImmutabilityUtil.canBlockBeDestroyed(world, blockPos)) {
                validBlocks.add(blockPos);
            }
        }
        
        if (!validBlocks.isEmpty()) {
            createBlockGroup(serverLevel, validBlocks, player, spellContext);
        }
    }
    
    private void createBlockGroup(ServerLevel level, List<BlockPos> blockPositions, Player player, SpellContext spellContext) {
        if (blockPositions.isEmpty()) {
            return;
        }
        
        java.util.Map<BlockPos, BlockState> capturedStates = new java.util.HashMap<>();
        for (BlockPos pos : blockPositions) {
            if (!level.isOutsideBuildHeight(pos)) {
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && !BlockImmutabilityUtil.isBlockImmutable(state)) {
                    capturedStates.put(pos, state);
                }
            }
        }
        
        List<BlockPos> validPositions = new ArrayList<>(capturedStates.keySet());
        
        ItemStack casterTool = spellContext.getCasterTool();
        MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
        
        List<BlockPos> filteredPositions = validPositions;
        if (context != null) {
            java.util.Set<BlockPos> claimedBlocks = new java.util.HashSet<>();
            for (SpellResult result : context.beginResults) {
                if (result != null && result.blockGroup != null && result.blockPositions != null) {
                    claimedBlocks.addAll(result.blockPositions);
                }
            }
            
            filteredPositions = validPositions.stream()
                .filter(pos -> !claimedBlocks.contains(pos))
                .toList();
            
            java.util.Map<BlockPos, BlockState> filteredStates = new java.util.HashMap<>();
            for (BlockPos pos : filteredPositions) {
                BlockState state = capturedStates.get(pos);
                if (state != null) {
                    filteredStates.put(pos, state);
                }
            }
            capturedStates = filteredStates;
        }
        
        if (filteredPositions.isEmpty()) {
            return;
        }
        
        Vec3 centerPos = calculateCenter(filteredPositions);
        
        BlockGroupEntity blockGroup = new BlockGroupEntity(ModEntities.BLOCK_GROUP.get(), level);
        blockGroup.setPos(centerPos.x, centerPos.y, centerPos.z);
        blockGroup.setCasterUUID(player.getUUID());
        
        blockGroup.addBlocksWithStates(filteredPositions, capturedStates);
        
        level.addFreshEntity(blockGroup);
        
        if (context != null) {
            SpellResult blockResult = SpellResult.fromBlockGroup(blockGroup, filteredPositions, player);
            boolean hasExistingBlockGroups = context.beginResults.stream()
                .anyMatch(r -> r != null && r.blockGroup != null);
            if (!hasExistingBlockGroups) {
                context.beginResults.clear();
            }
            context.beginResults.add(blockResult);
        }
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
            AugmentPierce.INSTANCE,
            AugmentSensitive.INSTANCE
        );
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAOE.INSTANCE, "Increases the area of blocks that can be selected");
        map.put(AugmentPierce.INSTANCE, "Increases the depth of blocks that can be selected");
        map.put(AugmentSensitive.INSTANCE, "Only selects entities, ignoring blocks.");
    }

    @Override
    public String getBookDescription() {
        return "Selects a target entity or block without performing any action. Use this to choose targets for future operations. AOE and Pierce allow selecting multiple blocks. For block translation, selected blocks are converted to a block group entity. Sensitive restricts selection to entities only.";
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

