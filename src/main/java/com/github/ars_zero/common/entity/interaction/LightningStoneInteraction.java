package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.LightningVoxelEntity;
import com.github.ars_zero.common.entity.StoneVoxelEntity;

public class LightningStoneInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(com.github.ars_zero.common.entity.BaseVoxelEntity primary, com.github.ars_zero.common.entity.BaseVoxelEntity secondary) {
        return (primary instanceof LightningVoxelEntity && secondary instanceof StoneVoxelEntity) ||
               (primary instanceof StoneVoxelEntity && secondary instanceof LightningVoxelEntity);
    }
    
    @Override
    public VoxelInteractionResult interact(com.github.ars_zero.common.entity.BaseVoxelEntity primary, com.github.ars_zero.common.entity.BaseVoxelEntity secondary) {
        boolean lightningIsPrimary = primary instanceof LightningVoxelEntity;
        return VoxelInteractionResult.builder(primary.position())
            .primaryAction(lightningIsPrimary ? VoxelInteractionResult.ActionType.DISCARD : VoxelInteractionResult.ActionType.CONTINUE)
            .secondaryAction(lightningIsPrimary ? VoxelInteractionResult.ActionType.CONTINUE : VoxelInteractionResult.ActionType.DISCARD)
            .build();
    }
}
