package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;

public interface VoxelInteraction {
    VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary);
    
    boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary);
}


