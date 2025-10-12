package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.VoxelEntity;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConjureVoxelEffect extends AbstractEffect {
    
    public static final String ID = "conjure_voxel_effect";
    public static final ConjureVoxelEffect INSTANCE = new ConjureVoxelEffect();

    public ConjureVoxelEffect() {
        super(ID, "Conjure Voxel");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world instanceof ServerLevel serverLevel) {
            Entity target = rayTraceResult.getEntity();
            Vec3 pos = target.position();
            
            int duration = getDuration(spellStats);
            VoxelEntity voxel = new VoxelEntity(serverLevel, pos.x, pos.y, pos.z, duration);
            serverLevel.addFreshEntity(voxel);
        }
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world instanceof ServerLevel serverLevel) {
            double aoeBuff = spellStats.getAoeMultiplier();
            List<BlockPos> posList = com.hollingsworth.arsnouveau.api.util.SpellUtil.calcAOEBlocks(shooter, rayTraceResult.getBlockPos(), rayTraceResult, aoeBuff, spellStats.getBuffCount(AugmentPierce.INSTANCE));
            
            int duration = getDuration(spellStats);
            
            for (BlockPos pos : posList) {
                Vec3 voxelPos = Vec3.atCenterOf(pos.relative(rayTraceResult.getDirection()));
                VoxelEntity voxel = new VoxelEntity(serverLevel, voxelPos.x, voxelPos.y, voxelPos.z, duration);
                serverLevel.addFreshEntity(voxel);
            }
        }
    }
    
    private int getDuration(SpellStats spellStats) {
        return (int) (200 * spellStats.getDurationMultiplier());
    }

    @Override
    public int getDefaultManaCost() {
        return 50;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentAOE.INSTANCE, AugmentPierce.INSTANCE, AugmentExtendTime.INSTANCE, AugmentSensitive.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        addBlockAoeAugmentDescriptions(map);
        map.put(AugmentSensitive.INSTANCE, "Places a voxel at a target entity's position.");
        map.put(AugmentExtendTime.INSTANCE, "Increases the duration the voxel remains.");
    }

    @Override
    public String getBookDescription() {
        return "Conjures a small voxel entity that persists for a duration. The voxel is a quarter the size of a normal block and does not collide with anything.";
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.ONE;
    }

    @NotNull
    @Override
    public Set<SpellSchool> getSchools() {
        return Set.of(SpellSchools.MANIPULATION);
    }

    @Override
    public ResourceLocation getRegistryName() {
        return ArsZero.prefix(ID);
    }
}
