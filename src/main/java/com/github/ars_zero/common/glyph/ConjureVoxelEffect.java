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

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world instanceof ServerLevel serverLevel) {
            Vec3 hitLocation = rayTraceResult.getLocation();
            int duration = getDuration(spellStats);
            
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
    
    private BaseVoxelEntity createVoxel(ServerLevel level, double x, double y, double z, int duration, SpellContext context) {
        boolean hasWater = false;
        boolean hasFire = false;
        
        SpellContext peekContext = context.clone();
        if (peekContext.hasNextPart()) {
            while (peekContext.hasNextPart()) {
                AbstractSpellPart next = peekContext.nextPart();
                if (next instanceof AbstractEffect) {
                    if (next == EffectConjureWater.INSTANCE) {
                        hasWater = true;
                        context.nextPart();
                    } else if (next == EffectIgnite.INSTANCE) {
                        hasFire = true;
                        context.nextPart();
                    }
                    break;
                }
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
        return Set.of(AugmentExtendTime.INSTANCE, AugmentSensitive.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentSensitive.INSTANCE, "Places a voxel at a target entity's position.");
        map.put(AugmentExtendTime.INSTANCE, "Increases the duration the voxel remains.");
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
