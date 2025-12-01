package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import com.github.ars_zero.common.entity.WindVoxelEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

public class WindWaterInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return (primary instanceof WindVoxelEntity && secondary instanceof WaterVoxelEntity) ||
               (primary instanceof WaterVoxelEntity && secondary instanceof WindVoxelEntity);
    }
    
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return VoxelInteractionResult.builder(primary.position())
            .particles(ParticleTypes.CLOUD, 16)
            .particles(ParticleTypes.SPLASH, 10)
            .sound(SoundEvents.BUCKET_FILL)
            .primaryAction(VoxelInteractionResult.ActionType.CONTINUE)
            .secondaryAction(VoxelInteractionResult.ActionType.CONTINUE)
            .build();
    }
}


