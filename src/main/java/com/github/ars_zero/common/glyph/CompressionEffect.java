package com.github.ars_zero.common.glyph;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.CompressibleVoxelEntity;
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

public class CompressionEffect extends AbstractEffect {
    
    public static final String ID = "compression_effect";
    public static final CompressionEffect INSTANCE = new CompressionEffect();
    
    private static final float MIN_SCALE = 0.3f;
    private static final float MAX_COMPRESSION = 0.8f;
    private static final float COMPRESSION_RATE = 0.02f;
    private static final float EMISSION_FACTOR = 2.0f;

    public CompressionEffect() {
        super(ID, "Compression");
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
            float baseSize = voxel.getBaseSize();
            
            float compressionRate = COMPRESSION_RATE + ((float)spellStats.getAmpMultiplier() * 0.01f);
            float compressionFactor = Math.min(1.0f - (currentSize / baseSize), MAX_COMPRESSION);
            
            float exponentialDecay = (float) Math.exp(-compressionFactor * 3.0);
            float newSize = baseSize * (MIN_SCALE + (1.0f - MIN_SCALE) * exponentialDecay);
            
            if (newSize < baseSize * MIN_SCALE) {
                newSize = baseSize * MIN_SCALE;
            }
            
            voxel.setSize(newSize);
            voxel.refreshDimensions();
            
            if (voxel instanceof com.github.ars_zero.common.entity.ArcaneVoxelEntity arcaneVoxel) {
                updateCompressionState(arcaneVoxel, compressionFactor);
            }
        }
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        ArsZero.LOGGER.debug("CompressionEffect: Block hit, effect only works on entities");
    }

    private void updateCompressionState(com.github.ars_zero.common.entity.ArcaneVoxelEntity voxel, float compressionFactor) {
        if (voxel instanceof CompressibleVoxelEntity compressibleVoxel) {
            compressibleVoxel.setCompressionLevel(compressionFactor);
            compressibleVoxel.setEmissiveIntensity(compressionFactor * EMISSION_FACTOR);
            
            if (compressionFactor > 0.6f) {
                compressibleVoxel.setDamageEnabled(true);
            }
        }
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
        map.put(AugmentAmplify.INSTANCE, "Increases the compression rate");
        map.put(AugmentDampen.INSTANCE, "Decreases the compression rate");
    }

    @Override
    public String getBookDescription() {
        return "Gradually compresses the target entity over time, reducing its size while increasing its magical intensity. Works best with Voxel entities when used with Temporal Context Form in the TICK phase. The entity shrinks using an exponential decay curve, with a minimum size of 30% of original. At high compression levels, the entity becomes more emissive and can deal damage.";
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