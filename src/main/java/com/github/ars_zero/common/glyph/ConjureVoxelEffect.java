package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ArcaneVoxelEntity;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.StaffCastContext;
import com.github.ars_zero.common.util.MathHelper;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectIgnite;
import com.alexthw.sauce.registry.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
    private static final int MAX_AMPLIFY_LEVEL = 2;
    private static final float BASE_VOXEL_SIZE = BaseVoxelEntity.DEFAULT_BASE_SIZE;
    private static final float AMPLIFY_SIZE_STEP = BaseVoxelEntity.DEFAULT_BASE_SIZE;

    public ConjureVoxelEffect() {
        super(ArsZero.prefix(ID), "Conjure Voxel");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world instanceof ServerLevel serverLevel) {
            Entity target = rayTraceResult.getEntity();
            Vec3 pos = target.position();
            
            int duration = getDuration(spellStats);
            int splitLevel = spellStats.getBuffCount(AugmentSplit.INSTANCE);
            int amplifyLevel = clampAmplifyLevel(spellStats);
            float voxelSize = getVoxelSize(amplifyLevel);
            float waterPower = getWaterPower(shooter);
            if (splitLevel > 0) {
                createSplitVoxels(serverLevel, pos.x, pos.y, pos.z, duration, spellContext, shooter, resolver, splitLevel, waterPower, voxelSize);
            } else {
                BaseVoxelEntity voxel = createVoxel(serverLevel, pos.x, pos.y, pos.z, duration, spellContext);
                applyVoxelSize(voxel, voxelSize);
                
                boolean isArcane = voxel instanceof ArcaneVoxelEntity;
                
                if (isArcane) {
                    SpellContext newContext = spellContext.makeChildContext();
                    spellContext.setCanceled(true);
                    voxel.setResolver(resolver.getNewResolver(newContext));
                } else {
                    voxel.setResolver(null);
                    SpellContext remainingContext = spellContext.makeChildContext();
                    spellContext.setCanceled(true);
                    
                    serverLevel.addFreshEntity(voxel);
                    
                    resolver.getNewResolver(remainingContext).onResolveEffect(serverLevel, new EntityHitResult(voxel));
                    return;
                }
                
                voxel.setCaster(shooter);
                if (voxel instanceof WaterVoxelEntity waterVoxel) {
                    waterVoxel.setCasterWaterPower(waterPower);
                }
                
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
            int amplifyLevel = clampAmplifyLevel(spellStats);
            float voxelSize = getVoxelSize(amplifyLevel);
            float waterPower = getWaterPower(shooter);
            if (splitLevel > 0) {
                createSplitVoxels(serverLevel, hitLocation.x, hitLocation.y, hitLocation.z, duration, spellContext, shooter, resolver, splitLevel, waterPower, voxelSize);
            } else {
                BaseVoxelEntity voxel = createVoxel(serverLevel, hitLocation.x, hitLocation.y, hitLocation.z, duration, spellContext);
                applyVoxelSize(voxel, voxelSize);
                
                boolean isArcane = voxel instanceof ArcaneVoxelEntity;
                
                if (isArcane) {
                    SpellContext newContext = spellContext.makeChildContext();
                    spellContext.setCanceled(true);
                    voxel.setResolver(resolver.getNewResolver(newContext));
                } else {
                    voxel.setResolver(null);
                    SpellContext remainingContext = spellContext.makeChildContext();
                    spellContext.setCanceled(true);
                    
                    serverLevel.addFreshEntity(voxel);
                    updateTemporalContext(shooter, voxel);
                    
                    resolver.getNewResolver(remainingContext).onResolveEffect(serverLevel, new EntityHitResult(voxel));
                    return;
                }
                
                voxel.setCaster(shooter);
                if (voxel instanceof WaterVoxelEntity waterVoxel) {
                    waterVoxel.setCasterWaterPower(waterPower);
                }
                
                if (voxel instanceof FireVoxelEntity || voxel instanceof ArcaneVoxelEntity) {
                    voxel.setNoGravityCustom(true);
                }
                
                serverLevel.addFreshEntity(voxel);
                updateTemporalContext(shooter, voxel);
            }
        }
    }
    
    private void createSplitVoxels(ServerLevel level, double x, double y, double z, int duration, SpellContext context, LivingEntity shooter, SpellResolver resolver, int splitLevel, float waterPower, float voxelSize) {
        int maxSplitLevel = 3;
        int actualSplitLevel = Math.min(splitLevel, maxSplitLevel);
        
        int entityCount;
        double circleRadius;
        
        switch (actualSplitLevel) {
            case 1:
                entityCount = 3;
                circleRadius = 0.35;
                break;
            case 2:
                entityCount = 5;
                circleRadius = 0.55;
                break;
            case 3:
                entityCount = 7;
                circleRadius = 0.75;
                break;
            default:
                entityCount = 1;
                circleRadius = 0.0;
                break;
        }
        circleRadius += voxelSize * 0.5;
        
        java.util.List<BaseVoxelEntity> createdVoxels = new java.util.ArrayList<>();
        boolean isArcane = true;
        
        if (entityCount == 1) {
            BaseVoxelEntity voxel = createVoxel(level, x, y, z, duration, context);
            applyVoxelSize(voxel, voxelSize);
            
            isArcane = voxel instanceof ArcaneVoxelEntity;
            
            if (isArcane) {
                SpellContext newContext = context.makeChildContext();
                context.setCanceled(true);
                voxel.setResolver(resolver.getNewResolver(newContext));
            } else {
                voxel.setResolver(null);
            }
            
            voxel.setCaster(shooter);
            if (voxel instanceof WaterVoxelEntity waterVoxel) {
                waterVoxel.setCasterWaterPower(waterPower);
            }
            
            if (voxel instanceof FireVoxelEntity || voxel instanceof ArcaneVoxelEntity) {
                voxel.setNoGravityCustom(true);
            }
            
            level.addFreshEntity(voxel);
            createdVoxels.add(voxel);
        } else {
            SpellContext peekContext = context.clone();
            boolean hasWater = false;
            boolean hasFire = false;
            
            while (peekContext.hasNextPart()) {
                AbstractSpellPart next = peekContext.nextPart();
                if (next instanceof AbstractEffect) {
                    if (next == EffectConjureWater.INSTANCE) {
                        hasWater = true;
                        hasFire = false;
                    } else if (next == EffectIgnite.INSTANCE) {
                        hasFire = true;
                        hasWater = false;
                    }
                    break;
                }
            }
            
            Vec3 center = new Vec3(x, y, z);
            Vec3 lookDirection = shooter.getLookAngle();
            java.util.List<Vec3> positions = MathHelper.getCirclePositions(center, lookDirection, circleRadius, entityCount);
            
            isArcane = !hasWater && !hasFire;
            SpellContext newContext = null;
            
            if (isArcane) {
                newContext = context.makeChildContext();
                context.setCanceled(true);
            }
            
            for (Vec3 pos : positions) {
                BaseVoxelEntity voxel;
                if (hasWater) {
                    voxel = new WaterVoxelEntity(level, pos.x, pos.y, pos.z, duration);
                } else if (hasFire) {
                    voxel = new FireVoxelEntity(level, pos.x, pos.y, pos.z, duration);
                } else {
                    voxel = new ArcaneVoxelEntity(level, pos.x, pos.y, pos.z, duration);
                }
                
                applyVoxelSize(voxel, voxelSize);
                
                voxel.setCaster(shooter);
                if (voxel instanceof WaterVoxelEntity waterVoxel) {
                    waterVoxel.setCasterWaterPower(waterPower);
                }
                
                if (isArcane) {
                    voxel.setResolver(resolver.getNewResolver(newContext));
                } else {
                    voxel.setResolver(null);
                }
                
                if (voxel instanceof FireVoxelEntity || voxel instanceof ArcaneVoxelEntity) {
                    voxel.setNoGravityCustom(true);
                }
                
                level.addFreshEntity(voxel);
                createdVoxels.add(voxel);
            }
        }
        
        updateTemporalContextMultiple(shooter, createdVoxels);
        
        if (!isArcane && context.hasNextPart()) {
            SpellContext remainingContext = context.makeChildContext();
            context.setCanceled(true);
            for (BaseVoxelEntity voxel : createdVoxels) {
                resolver.getNewResolver(remainingContext.clone()).onResolveEffect(level, new EntityHitResult(voxel));
            }
        }
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
    
    private int clampAmplifyLevel(SpellStats spellStats) {
        int amplifyLevel = spellStats.getBuffCount(AugmentAmplify.INSTANCE);
        return Math.min(amplifyLevel, MAX_AMPLIFY_LEVEL);
    }
    
    private float getVoxelSize(int amplifyLevel) {
        int clamped = Math.max(0, Math.min(amplifyLevel, MAX_AMPLIFY_LEVEL));
        return BASE_VOXEL_SIZE + (AMPLIFY_SIZE_STEP * clamped);
    }
    
    private void applyVoxelSize(BaseVoxelEntity voxel, float size) {
        voxel.setSize(size);
        voxel.refreshDimensions();
    }
    
    private float getWaterPower(LivingEntity shooter) {
        if (shooter instanceof net.minecraft.world.entity.player.Player player) {
            AttributeInstance instance = player.getAttribute(ModRegistry.WATER_POWER);
            if (instance != null) {
                return (float) instance.getValue();
            }
        }
        return 0.0f;
    }
    
    private void updateTemporalContext(LivingEntity shooter, BaseVoxelEntity voxel) {
        if (!(shooter instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        
        StaffCastContext context = AbstractSpellStaff.getStaffContext(player);
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
        
        StaffCastContext context = AbstractSpellStaff.getStaffContext(player);
        if (context == null) {
            return;
        }
        
        if (!context.beginResults.isEmpty()) {
            for (SpellResult oldResult : context.beginResults) {
                if (oldResult.targetEntity instanceof BaseVoxelEntity oldVoxel && oldVoxel.isAlive()) {
                    oldVoxel.discard();
                }
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
        return 5;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE, AugmentSensitive.INSTANCE, AugmentSplit.INSTANCE);
    }

    @Override
    protected void addDefaultAugmentLimits(java.util.Map<ResourceLocation, Integer> defaults) {
        defaults.put(AugmentAmplify.INSTANCE.getRegistryName(), MAX_AMPLIFY_LEVEL);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "Increases the voxel's size up to level 2, boosting water output.");
        map.put(AugmentSensitive.INSTANCE, "Places a voxel at a target entity's position.");
        map.put(AugmentExtendTime.INSTANCE, "Increases the duration the voxel remains.");
        map.put(AugmentSplit.INSTANCE, "Splits the voxel into multiple identical entities without changing their size.");
    }

    @Override
    public String getBookDescription() {
        return "Conjures a compact 3x3x3 pixel purple voxel entity that persists for 1 minute. Amplify increases its size (up to level 2), which also boosts the amount of water a water voxel can place. The voxel does not collide with anything and can be grown using temporal effects like Enlarge. Arcane voxels carry and resolve all following effects on impact. Water and Fire voxels act as delimiters - they do not carry effects, allowing subsequent spells to target the voxel itself.";
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
}
