package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.IceVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

public class IceWaterInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return (primary instanceof IceVoxelEntity && secondary instanceof WaterVoxelEntity) ||
               (primary instanceof WaterVoxelEntity && secondary instanceof IceVoxelEntity);
    }
    
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        boolean iceIsPrimary = primary instanceof IceVoxelEntity;
        
        return VoxelInteractionResult.builder(primary.position())
            .particles(ParticleTypes.SPLASH, 12)
            .sound(SoundEvents.BUCKET_EMPTY)
            .primaryAction(iceIsPrimary ? VoxelInteractionResult.ActionType.CONTINUE : VoxelInteractionResult.ActionType.DISCARD)
            .secondaryAction(iceIsPrimary ? VoxelInteractionResult.ActionType.DISCARD : VoxelInteractionResult.ActionType.CONTINUE)
            .build();
    }
}
