package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.LightningVoxelEntity;
import net.minecraft.core.particles.ParticleTypes;

public class LightningLightningInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(com.github.ars_zero.common.entity.BaseVoxelEntity primary, com.github.ars_zero.common.entity.BaseVoxelEntity secondary) {
        return primary instanceof LightningVoxelEntity && secondary instanceof LightningVoxelEntity;
    }
    
    @Override
    public VoxelInteractionResult interact(com.github.ars_zero.common.entity.BaseVoxelEntity primary, com.github.ars_zero.common.entity.BaseVoxelEntity secondary) {
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
