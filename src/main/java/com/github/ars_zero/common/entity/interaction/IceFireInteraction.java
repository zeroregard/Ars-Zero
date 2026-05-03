package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.IceVoxelEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

public class IceFireInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return (primary instanceof IceVoxelEntity && secondary instanceof FireVoxelEntity) ||
               (primary instanceof FireVoxelEntity && secondary instanceof IceVoxelEntity);
    }
    
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        Vec3 position = primary.position();
        boolean iceIsPrimary = primary instanceof IceVoxelEntity;
        return VoxelInteractionResult.builder(position)
            .particles(ParticleTypes.CLOUD, 20)
            .sound(SoundEvents.FIRE_EXTINGUISH)
            .primaryAction(iceIsPrimary ? VoxelInteractionResult.ActionType.CONTINUE : VoxelInteractionResult.ActionType.DISCARD)
            .secondaryAction(iceIsPrimary ? VoxelInteractionResult.ActionType.DISCARD : VoxelInteractionResult.ActionType.CONTINUE)
            .build();
    }
}
