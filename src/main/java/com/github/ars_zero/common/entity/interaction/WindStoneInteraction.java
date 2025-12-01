package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.StoneVoxelEntity;
import com.github.ars_zero.common.entity.WindVoxelEntity;
import net.minecraft.core.particles.ParticleTypes;

public class WindStoneInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return (primary instanceof WindVoxelEntity && secondary instanceof StoneVoxelEntity) ||
               (primary instanceof StoneVoxelEntity && secondary instanceof WindVoxelEntity);
    }
    
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        boolean windIsPrimary = primary instanceof WindVoxelEntity;
        
        return VoxelInteractionResult.builder(primary.position())
            .particles(ParticleTypes.CLOUD, 12)
            .primaryAction(windIsPrimary ? VoxelInteractionResult.ActionType.DISCARD : VoxelInteractionResult.ActionType.CONTINUE)
            .secondaryAction(windIsPrimary ? VoxelInteractionResult.ActionType.CONTINUE : VoxelInteractionResult.ActionType.DISCARD)
            .build();
    }
}

