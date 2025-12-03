package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.LightningVoxelEntity;

public class LightningLightningInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(com.github.ars_zero.common.entity.BaseVoxelEntity primary, com.github.ars_zero.common.entity.BaseVoxelEntity secondary) {
        return primary instanceof LightningVoxelEntity && secondary instanceof LightningVoxelEntity;
    }
    
    @Override
    public VoxelInteractionResult interact(com.github.ars_zero.common.entity.BaseVoxelEntity primary, com.github.ars_zero.common.entity.BaseVoxelEntity secondary) {
        return VoxelInteractionResult.builder()
            .primaryAction(VoxelInteractionResult.ActionType.DISCARD)
            .secondaryAction(VoxelInteractionResult.ActionType.DISCARD)
            .build();
    }
}
