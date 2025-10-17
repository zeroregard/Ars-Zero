package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class EnlargeEffect extends AbstractEffect {
    
    public static final String ID = "enlarge_effect";
    public static final EnlargeEffect INSTANCE = new EnlargeEffect();

    public EnlargeEffect() {
        super(ID, "Enlarge");
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;
        
        Entity target = rayTraceResult.getEntity();
        
        if (target == null || !target.isAlive()) {
            return;
        }
        
        if (target instanceof BaseVoxelEntity voxel) {
            float currentSize = voxel.getSize();
            float growthRate = 0.01f + ((float)spellStats.getAmpMultiplier() * 0.005f);
            float newSize = currentSize * (1.0f + growthRate);
            
            float maxSize = 16.0f;
            if (newSize > maxSize) {
                newSize = maxSize;
            }
            
            voxel.setSize(newSize);
            voxel.refreshDimensions();
        }
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        ArsZero.LOGGER.debug("EnlargeEffect: Block hit, effect only works on entities");
    }

    @Override
    public int getDefaultManaCost() {
        return 1;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "Increases the growth rate");
        map.put(AugmentDampen.INSTANCE, "Decreases the growth rate");
    }

    @Override
    public String getBookDescription() {
        return "Gradually enlarges the target entity over time. Works best with Voxel entities when used with Temporal Context Form in the TICK phase. The entity grows by 1% per tick by default, with a maximum size of 16 blocks.";
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

