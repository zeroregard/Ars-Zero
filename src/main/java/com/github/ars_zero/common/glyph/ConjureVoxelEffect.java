package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.ArcaneVoxelEntity;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.IceVoxelEntity;
import com.github.ars_zero.common.entity.StoneVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.common.entity.WindVoxelEntity;
import com.github.ars_zero.common.item.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.item.AbstractSpellStaff;
import com.github.ars_zero.common.spell.ISubsequentEffectProvider;
import com.github.ars_zero.common.spell.SpellEffectType;
import com.github.ars_zero.common.spell.SpellResult;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
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
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectIgnite;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectWindshear;
import alexthw.ars_elemental.common.glyphs.EffectConjureTerrain;
import alexthw.ars_elemental.common.glyphs.EffectColdSnap;
import com.alexthw.sauce.registry.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class ConjureVoxelEffect extends AbstractEffect implements ISubsequentEffectProvider {
    
    public static final String ID = "conjure_voxel_effect";
    public static final ConjureVoxelEffect INSTANCE = new ConjureVoxelEffect();
    private static final int MAX_AMPLIFY_LEVEL = 2;
    private static final float BASE_VOXEL_SIZE = BaseVoxelEntity.DEFAULT_BASE_SIZE;
    private static final float AMPLIFY_SIZE_STEP = BaseVoxelEntity.DEFAULT_BASE_SIZE;
    private static final Map<AbstractEffect, VoxelVariant> VARIANT_CACHE = new IdentityHashMap<>();
    private static final ResourceLocation[] SUBSEQUENT_GLYPHS = new ResourceLocation[]{
        EffectConjureWater.INSTANCE.getRegistryName(),
        EffectIgnite.INSTANCE.getRegistryName(),
        EffectWindshear.INSTANCE.getRegistryName(),
        EffectConjureTerrain.INSTANCE.getRegistryName(),
        EffectColdSnap.INSTANCE.getRegistryName()
    };
    
    static {
        VARIANT_CACHE.put(EffectConjureWater.INSTANCE, VoxelVariant.WATER);
        VARIANT_CACHE.put(EffectIgnite.INSTANCE, VoxelVariant.FIRE);
        VARIANT_CACHE.put(EffectWindshear.INSTANCE, VoxelVariant.WIND);
        VARIANT_CACHE.put(EffectConjureTerrain.INSTANCE, VoxelVariant.STONE);
        VARIANT_CACHE.put(EffectColdSnap.INSTANCE, VoxelVariant.ICE);
    }

    public ConjureVoxelEffect() {
        super(ArsZero.prefix(ID), "Conjure Voxel");
    }

    @Override
    public ResourceLocation[] getSubsequentEffectGlyphs() {
        return SUBSEQUENT_GLYPHS;
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
                
                if (voxel instanceof FireVoxelEntity || voxel instanceof ArcaneVoxelEntity || voxel instanceof WindVoxelEntity) {
                    voxel.setNoGravityCustom(true);
                }
                if (voxel instanceof IceVoxelEntity || voxel instanceof StoneVoxelEntity) {
                    voxel.setNoGravityCustom(false);
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
                    updateTemporalContext(shooter, voxel, spellContext);
                    
                    resolver.getNewResolver(remainingContext).onResolveEffect(serverLevel, new EntityHitResult(voxel));
                    return;
                }
                
                voxel.setCaster(shooter);
                if (voxel instanceof WaterVoxelEntity waterVoxel) {
                    waterVoxel.setCasterWaterPower(waterPower);
                }
                
                if (voxel instanceof FireVoxelEntity || voxel instanceof ArcaneVoxelEntity || voxel instanceof WindVoxelEntity) {
                    voxel.setNoGravityCustom(true);
                }
                if (voxel instanceof IceVoxelEntity || voxel instanceof StoneVoxelEntity) {
                    voxel.setNoGravityCustom(false);
                }
                
                serverLevel.addFreshEntity(voxel);
                updateTemporalContext(shooter, voxel, spellContext);
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
            
            if (voxel instanceof FireVoxelEntity || voxel instanceof ArcaneVoxelEntity || voxel instanceof WindVoxelEntity) {
                voxel.setNoGravityCustom(true);
            }
            
            level.addFreshEntity(voxel);
            createdVoxels.add(voxel);
        } else {
            VoxelVariant variant = resolveVariantFromContext(context, false);
            
            Vec3 center = new Vec3(x, y, z);
            Vec3 lookDirection = shooter.getLookAngle();
            java.util.List<Vec3> positions = MathHelper.getCirclePositions(center, lookDirection, circleRadius, entityCount);
            
            isArcane = variant == VoxelVariant.ARCANE;
            SpellContext newContext = null;
            
            if (isArcane) {
                newContext = context.makeChildContext();
                context.setCanceled(true);
            }
            
            for (Vec3 pos : positions) {
                BaseVoxelEntity voxel = instantiateVariant(level, pos.x, pos.y, pos.z, duration, variant);
                
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
                
                if (voxel instanceof FireVoxelEntity || voxel instanceof ArcaneVoxelEntity || voxel instanceof WindVoxelEntity) {
                    voxel.setNoGravityCustom(true);
                }
                if (voxel instanceof IceVoxelEntity || voxel instanceof StoneVoxelEntity) {
                    voxel.setNoGravityCustom(false);
                }
                
                level.addFreshEntity(voxel);
                createdVoxels.add(voxel);
            }
        }
        
        updateTemporalContextMultiple(shooter, createdVoxels, context);
        
        if (!isArcane && context.hasNextPart()) {
            SpellContext remainingContext = context.makeChildContext();
            context.setCanceled(true);
            for (BaseVoxelEntity voxel : createdVoxels) {
                resolver.getNewResolver(remainingContext.clone()).onResolveEffect(level, new EntityHitResult(voxel));
            }
        }
    }
    
    private BaseVoxelEntity createVoxel(ServerLevel level, double x, double y, double z, int duration, SpellContext context) {
        VoxelVariant variant = resolveVariantFromContext(context, true);
        return instantiateVariant(level, x, y, z, duration, variant);
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
    
    private VoxelVariant resolveVariantFromContext(SpellContext context, boolean consume) {
        SpellContext iterator = context.clone();
        while (iterator.hasNextPart()) {
            AbstractSpellPart next = iterator.nextPart();
            if (next instanceof AbstractEffect effect) {
                VoxelVariant variant = resolveVariant(effect);
                if (variant != VoxelVariant.ARCANE && consume) {
                    consumeEffect(context, effect);
                }
                return variant;
            }
        }
        return VoxelVariant.ARCANE;
    }
    
    private VoxelVariant resolveVariant(AbstractEffect effect) {
        VoxelVariant cached = VARIANT_CACHE.get(effect);
        if (cached != null) {
            return cached;
        }
        return VoxelVariant.ARCANE;
    }
    
    private void consumeEffect(SpellContext context, AbstractEffect targetEffect) {
        ResourceLocation targetId = targetEffect.getRegistryName();
        while (context.hasNextPart()) {
            AbstractSpellPart consumed = context.nextPart();
            if (consumed instanceof AbstractEffect consumedEffect && effectsMatch(consumedEffect, targetEffect, targetId)) {
                break;
            }
        }
    }
    
    private boolean effectsMatch(AbstractEffect candidate, AbstractEffect reference, ResourceLocation id) {
        if (candidate == reference) {
            return true;
        }
        ResourceLocation candidateId = candidate.getRegistryName();
        return id != null && id.equals(candidateId);
    }
    
    private BaseVoxelEntity instantiateVariant(ServerLevel level, double x, double y, double z, int duration, VoxelVariant variant) {
        return switch (variant) {
            case WATER -> new WaterVoxelEntity(level, x, y, z, duration);
            case FIRE -> new FireVoxelEntity(level, x, y, z, duration);
            case STONE -> new StoneVoxelEntity(level, x, y, z, duration);
            case WIND -> new WindVoxelEntity(level, x, y, z, duration);
            case ICE -> new IceVoxelEntity(level, x, y, z, duration);
            default -> new ArcaneVoxelEntity(level, x, y, z, duration);
        };
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
    
    private void updateTemporalContext(LivingEntity shooter, BaseVoxelEntity voxel, SpellContext spellContext) {
        if (!(shooter instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        
        ItemStack casterTool = spellContext.getCasterTool();
        MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
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
    
    private void updateTemporalContextMultiple(LivingEntity shooter, java.util.List<BaseVoxelEntity> voxels, SpellContext spellContext) {
        if (!(shooter instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        
        ItemStack casterTool = spellContext.getCasterTool();
        MultiPhaseCastContext context = AbstractMultiPhaseCastDevice.findContextByStack(player, casterTool);
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
    
    private enum VoxelVariant {
        ARCANE,
        WATER,
        FIRE,
        STONE,
        WIND,
        ICE
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
        return Set.of(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE, AugmentSplit.INSTANCE);
    }

    @Override
    protected void addDefaultAugmentLimits(java.util.Map<ResourceLocation, Integer> defaults) {
        defaults.put(AugmentAmplify.INSTANCE.getRegistryName(), MAX_AMPLIFY_LEVEL);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "Increases the size of the voxel");
        map.put(AugmentExtendTime.INSTANCE, "Increases the duration the voxel remains.");
        map.put(AugmentSplit.INSTANCE, "Splits the voxel into multiples");
    }

    @Override
    public String getBookDescription() {
        return "Conjures a magic voxel entity that persists for some time. Possible effect augments via: 'Conjure Water', 'Ignite', 'Wind Shear', 'Conjure Terrain', & 'Cold Snap'";
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
