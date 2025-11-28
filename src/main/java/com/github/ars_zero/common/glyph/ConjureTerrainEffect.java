package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ConjureTerrainEffect extends AbstractEffect {
    
    public static final String ID = "conjure_terrain_effect";
    public static final ConjureTerrainEffect INSTANCE = new ConjureTerrainEffect();
    private static final int MAX_RADIUS = 3;
    private static final int MAX_LAYERS = 3;
    
    public ConjureTerrainEffect() {
        super(ArsZero.prefix(ID), "Conjure Terrain");
    }
    
    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos origin = rayTraceResult.getBlockPos();
        reinforceArea(serverLevel, origin, spellStats);
    }
    
    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos origin = BlockPos.containing(rayTraceResult.getLocation());
        reinforceArea(serverLevel, origin, spellStats);
    }
    
    private void reinforceArea(ServerLevel level, BlockPos center, SpellStats stats) {
        int radius = Math.min(MAX_RADIUS, 1 + stats.getBuffCount(AugmentAmplify.INSTANCE));
        int layers = Math.min(MAX_LAYERS, 1 + stats.getBuffCount(AugmentExtendTime.INSTANCE));
        
        for (int dy = 0; dy < layers; dy++) {
            BlockPos layerOrigin = center.below(dy);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > radius + dy) {
                        continue;
                    }
                    BlockPos target = layerOrigin.offset(dx, 0, dz);
                    BlockState state = level.getBlockState(target);
                    if (state.isAir() || state.canBeReplaced()) {
                        level.setBlock(target, Blocks.STONE.defaultBlockState(), 3);
                        continue;
                    }
                    float hardness = state.getDestroySpeed(level, target);
                    if (hardness >= 0.0f && hardness <= 1.5f) {
                        level.setBlock(target, Blocks.COBBLESTONE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
    
    @Override
    public int getDefaultManaCost() {
        return 45;
    }
    
    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE);
    }
    
    @Override
    public void addAugmentDescriptions(java.util.Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "Expands the radius of placed terrain.");
        map.put(AugmentExtendTime.INSTANCE, "Adds additional downward layers.");
    }
    
    @Override
    public String getBookDescription() {
        return "Condenses loose blocks into stone or cobblestone, letting your spell sketch makeshift cover or platforms on demand.";
    }
    
    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }
    
    @NotNull
    @Override
    public Set<SpellSchool> getSchools() {
        return Set.of(SpellSchools.MANIPULATION);
    }
}
