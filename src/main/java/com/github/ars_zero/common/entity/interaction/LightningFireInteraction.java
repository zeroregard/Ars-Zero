package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.LightningVoxelEntity;

public class LightningFireInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(com.github.ars_zero.common.entity.BaseVoxelEntity primary, com.github.ars_zero.common.entity.BaseVoxelEntity secondary) {
        return (primary instanceof LightningVoxelEntity && secondary instanceof FireVoxelEntity) ||
               (primary instanceof FireVoxelEntity && secondary instanceof LightningVoxelEntity);
    }
    
    @Override
    public VoxelInteractionResult interact(com.github.ars_zero.common.entity.BaseVoxelEntity primary, com.github.ars_zero.common.entity.BaseVoxelEntity secondary) {
        boolean lightningIsPrimary = primary instanceof LightningVoxelEntity;
        return VoxelInteractionResult.builder()
            .primaryAction(lightningIsPrimary ? VoxelInteractionResult.ActionType.CONTINUE : VoxelInteractionResult.ActionType.DISCARD)
            .secondaryAction(lightningIsPrimary ? VoxelInteractionResult.ActionType.DISCARD : VoxelInteractionResult.ActionType.CONTINUE)
            .build();
    }
}
