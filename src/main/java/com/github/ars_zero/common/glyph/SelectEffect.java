package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.common.item.ArsZeroStaff;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.StaffCastContext;
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
import net.minecraft.world.level.Level;
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
        
        ArsZero.LOGGER.debug("Selected entity: {}", rayTraceResult.getEntity().getName().getString());
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;
        if (!(shooter instanceof Player player)) return;
        if (!(world instanceof ServerLevel serverLevel)) return;
        
        ArsZero.LOGGER.info("[SelectEffect] Block hit at {}", rayTraceResult.getBlockPos());
        
        if (spellStats.isSensitive()) {
            ArsZero.LOGGER.debug("[SelectEffect] Spell is sensitive, ignoring blocks");
            return;
        }
        
        if (!BlockUtil.destroyRespectsClaim(getPlayer(shooter, serverLevel), world, rayTraceResult.getBlockPos())) {
            ArsZero.LOGGER.debug("[SelectEffect] Block claim check failed");
            return;
        }
        
        BlockPos pos = rayTraceResult.getBlockPos();
        double aoeBuff = spellStats.getAoeMultiplier();
        int pierceBuff = spellStats.getBuffCount(AugmentPierce.INSTANCE);
        List<BlockPos> posList = SpellUtil.calcAOEBlocks(shooter, pos, rayTraceResult, aoeBuff, pierceBuff);
        
        ArsZero.LOGGER.info("[SelectEffect] Calculated {} AOE blocks (AOE={}, Pierce={})", posList.size(), aoeBuff, pierceBuff);
        
        List<BlockPos> validBlocks = new ArrayList<>();
        for (BlockPos blockPos : posList) {
            if (!world.isOutsideBuildHeight(blockPos) && BlockUtil.destroyRespectsClaim(getPlayer(shooter, serverLevel), world, blockPos)) {
                validBlocks.add(blockPos);
                ArsZero.LOGGER.debug("[SelectEffect] Valid block at: {}", blockPos);
            }
        }
        
        ArsZero.LOGGER.info("[SelectEffect] Found {} valid blocks to select", validBlocks.size());
        
        if (!validBlocks.isEmpty()) {
            ArsZero.LOGGER.info("[SelectEffect] Creating BlockGroupEntity with {} blocks", validBlocks.size());
            createBlockGroup(serverLevel, validBlocks, player);
        }
    }
    
    private void createBlockGroup(ServerLevel level, List<BlockPos> blockPositions, Player player) {
        if (blockPositions.isEmpty()) {
            ArsZero.LOGGER.warn("[SelectEffect] createBlockGroup called with empty block list");
            return;
        }
        
        Vec3 centerPos = calculateCenter(blockPositions);
        ArsZero.LOGGER.info("[SelectEffect] Creating BlockGroupEntity at center: {}", centerPos);
        
        BlockGroupEntity blockGroup = new BlockGroupEntity(ModEntities.BLOCK_GROUP.get(), level);
        blockGroup.setPos(centerPos.x, centerPos.y, centerPos.z);
        
        blockGroup.addBlocks(blockPositions);
        ArsZero.LOGGER.info("[SelectEffect] BlockGroupEntity added {} blocks", blockGroup.getBlockCount());
        
        blockGroup.removeOriginalBlocks();
        ArsZero.LOGGER.info("[SelectEffect] Removed original blocks from world");
        
        level.addFreshEntity(blockGroup);
        ArsZero.LOGGER.info("[SelectEffect] BlockGroupEntity spawned with ID {}", blockGroup.getId());
        
        StaffCastContext context = ArsZeroStaff.getStaffContext(player);
        if (context != null) {
            SpellResult blockResult = SpellResult.fromBlockGroup(blockGroup, blockPositions, player);
            context.beginResults.clear();
            context.beginResults.add(blockResult);
            ArsZero.LOGGER.info("[SelectEffect] Added BlockGroupEntity to beginResults. BlockGroup: {}, BlockPositions: {}", 
                blockResult.blockGroup != null ? blockResult.blockGroup.getId() : "null",
                blockResult.blockPositions != null ? blockResult.blockPositions.size() : "null");
        } else {
            ArsZero.LOGGER.warn("[SelectEffect] StaffCastContext is null for player {}", player.getName().getString());
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
        return 5;
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
        addBlockAoeAugmentDescriptions(map);
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

