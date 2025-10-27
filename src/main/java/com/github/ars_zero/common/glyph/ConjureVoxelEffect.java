package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ArcaneVoxelEntity;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.common.item.ArsZeroStaff;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.StaffCastContext;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectIgnite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

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
            int splitLevel = spellStats.getBuffCount(AugmentSplit.INSTANCE);
            if (splitLevel > 0) {
                createSplitVoxels(serverLevel, pos.x, pos.y, pos.z, duration, spellContext, shooter, resolver, splitLevel);
            } else {
                BaseVoxelEntity voxel = createVoxel(serverLevel, pos.x, pos.y, pos.z, duration, spellContext);
                
                SpellContext newContext = spellContext.makeChildContext();
                spellContext.setCanceled(true);
                
                voxel.setCaster(shooter);
                voxel.setResolver(resolver.getNewResolver(newContext));
                
                if (voxel instanceof FireVoxelEntity || voxel instanceof ArcaneVoxelEntity) {
                    voxel.setNoGravityCustom(true);
                }
                
                serverLevel.addFreshEntity(voxel);
            }
        }
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world instanceof ServerLevel serverLevel) {
            Vec3 hitLocation = rayTraceResult.getLocation();
            int duration = getDuration(spellStats);
            
            int splitLevel = spellStats.getBuffCount(AugmentSplit.INSTANCE);
            if (splitLevel > 0) {
                createSplitVoxels(serverLevel, hitLocation.x, hitLocation.y, hitLocation.z, duration, spellContext, shooter, resolver, splitLevel);
            } else {
                BaseVoxelEntity voxel = createVoxel(serverLevel, hitLocation.x, hitLocation.y, hitLocation.z, duration, spellContext);
                
                SpellContext newContext = spellContext.makeChildContext();
                spellContext.setCanceled(true);
                
                voxel.setCaster(shooter);
                voxel.setResolver(resolver.getNewResolver(newContext));
                
                if (voxel instanceof FireVoxelEntity || voxel instanceof ArcaneVoxelEntity) {
                    voxel.setNoGravityCustom(true);
                }
                
                serverLevel.addFreshEntity(voxel);
                updateTemporalContext(shooter, voxel);
            }
        }
    }
    
    private void createSplitVoxels(ServerLevel level, double x, double y, double z, int duration, SpellContext context, LivingEntity shooter, SpellResolver resolver, int splitLevel) {
        int maxSplitLevel = 3;
        int actualSplitLevel = Math.min(splitLevel, maxSplitLevel);
        
        int entityCount;
        float size;
        
        switch (actualSplitLevel) {
            case 1:
                entityCount = 3;
                size = 0.1875f; // 3x3x3 pixels (3/16 of a block)
                break;
            case 2:
                entityCount = 5;
                size = 0.125f; // 2x2x2 pixels (2/16 of a block)
                break;
            case 3:
                entityCount = 7;
                size = 0.0625f; // 1x1x1 pixels (1/16 of a block)
                break;
            default:
                entityCount = 1;
                size = 0.25f; // 4x4x4 pixels (4/16 of a block)
                break;
        }
        
        java.util.List<BaseVoxelEntity> createdVoxels = new java.util.ArrayList<>();
        
        for (int i = 0; i < entityCount; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;
            
            BaseVoxelEntity voxel = createVoxel(level, x + offsetX, y + offsetY, z + offsetZ, duration, context);
            voxel.setSize(size);
            voxel.refreshDimensions();
            
            SpellContext newContext = context.makeChildContext();
            context.setCanceled(true);
            
            voxel.setCaster(shooter);
            voxel.setResolver(resolver.getNewResolver(newContext));
            
            if (voxel instanceof FireVoxelEntity || voxel instanceof ArcaneVoxelEntity) {
                voxel.setNoGravityCustom(true);
            }
            
            level.addFreshEntity(voxel);
            createdVoxels.add(voxel);
        }
        
        updateTemporalContextMultiple(shooter, createdVoxels);
    }
    
    private BaseVoxelEntity createVoxel(ServerLevel level, double x, double y, double z, int duration, SpellContext context) {
        boolean hasWater = false;
        boolean hasFire = false;
        
        SpellContext peekContext = context.clone();
        while (peekContext.hasNextPart()) {
            AbstractSpellPart next = peekContext.nextPart();
            if (next instanceof AbstractEffect) {
                if (next == EffectConjureWater.INSTANCE) {
                    hasWater = true;
                    while (context.hasNextPart()) {
                        AbstractSpellPart consumed = context.nextPart();
                        if (consumed == EffectConjureWater.INSTANCE) {
                            break;
                        }
                    }
                } else if (next == EffectIgnite.INSTANCE) {
                    hasFire = true;
                    while (context.hasNextPart()) {
                        AbstractSpellPart consumed = context.nextPart();
                        if (consumed == EffectIgnite.INSTANCE) {
                            break;
                        }
                    }
                }
                break;
            }
        }
        
        BaseVoxelEntity result;
        if (hasWater) {
            result = new WaterVoxelEntity(level, x, y, z, duration);
        } else if (hasFire) {
            result = new FireVoxelEntity(level, x, y, z, duration);
        } else {
            result = new ArcaneVoxelEntity(level, x, y, z, duration);
        }
        
        return result;
    }
    
    private void updateTemporalContext(LivingEntity shooter, BaseVoxelEntity voxel) {
        if (!(shooter instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        
        StaffCastContext context = ArsZeroStaff.getStaffContext(player);
        if (context == null) {
            return;
        }
        
        if (!context.beginResults.isEmpty()) {
            SpellResult oldResult = context.beginResults.get(0);
            if (oldResult.targetEntity instanceof ArcaneVoxelEntity oldVoxel && oldVoxel.isAlive()) {
                oldVoxel.discard();
            }
        }
        
        SpellResult voxelResult = SpellResult.fromHitResultWithCaster(
            new net.minecraft.world.phys.EntityHitResult(voxel), 
            SpellEffectType.RESOLVED, 
            player
        );
        
        context.beginResults.clear();
        context.beginResults.add(voxelResult);
    }
    
    private void updateTemporalContextMultiple(LivingEntity shooter, java.util.List<BaseVoxelEntity> voxels) {
        if (!(shooter instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        
        StaffCastContext context = ArsZeroStaff.getStaffContext(player);
        if (context == null) {
            return;
        }
        
        if (!context.beginResults.isEmpty()) {
            SpellResult oldResult = context.beginResults.get(0);
            if (oldResult.targetEntity instanceof ArcaneVoxelEntity oldVoxel && oldVoxel.isAlive()) {
                oldVoxel.discard();
            }
        }
        
        context.beginResults.clear();
        
        for (BaseVoxelEntity voxel : voxels) {
            SpellResult voxelResult = SpellResult.fromHitResultWithCaster(
                new net.minecraft.world.phys.EntityHitResult(voxel), 
                SpellEffectType.RESOLVED, 
                player
            );
            context.beginResults.add(voxelResult);
        }
    }
    
    private int getDuration(SpellStats spellStats) {
        double durationMultiplier = spellStats.getDurationMultiplier();
        if (durationMultiplier <= 0) {
            durationMultiplier = 1.0;
        }
        return (int) (1200 * durationMultiplier);
    }

    @Override
    public int getDefaultManaCost() {
        return 50;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentExtendTime.INSTANCE, AugmentSensitive.INSTANCE, AugmentSplit.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentSensitive.INSTANCE, "Places a voxel at a target entity's position.");
        map.put(AugmentExtendTime.INSTANCE, "Increases the duration the voxel remains.");
        map.put(AugmentSplit.INSTANCE, "Splits the voxel into multiple smaller entities.");
    }

    @Override
    public String getBookDescription() {
        return "Conjures a small 4x4x4 pixel (1/4 block) purple voxel entity that persists for 1 minute. The voxel does not collide with anything and can be grown using temporal effects like Enlarge.";
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
