package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.WindVoxelEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

public class WindFireInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return (primary instanceof WindVoxelEntity && secondary instanceof FireVoxelEntity) ||
               (primary instanceof FireVoxelEntity && secondary instanceof WindVoxelEntity);
    }
    
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return VoxelInteractionResult.builder(primary.position())
            .particles(ParticleTypes.CLOUD, 20)
            .particles(ParticleTypes.FLAME, 10)
            .sound(SoundEvents.GENERIC_EXPLODE.value())
            .primaryAction(VoxelInteractionResult.ActionType.DISCARD)
            .secondaryAction(VoxelInteractionResult.ActionType.DISCARD)
            .build();
    }
}


