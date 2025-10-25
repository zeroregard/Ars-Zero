package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import net.minecraft.core.particles.ParticleTypes;

public class MergeInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return primary.getClass() == secondary.getClass();
    }
    
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        float primarySize = primary.getSize();
        float secondarySize = secondary.getSize();
        
        float primaryVolume = primarySize * primarySize * primarySize;
        float secondaryVolume = secondarySize * secondarySize * secondarySize;
        float totalVolume = primaryVolume + secondaryVolume;
        
        float newSize = (float) Math.cbrt(totalVolume);
        
        return VoxelInteractionResult.builder(primary.position())
            .primaryAction(VoxelInteractionResult.ActionType.RESIZE)
            .primaryNewSize(newSize)
            .secondaryAction(VoxelInteractionResult.ActionType.DISCARD)
            .particles(ParticleTypes.POOF, 10)
            .build();
    }
}

