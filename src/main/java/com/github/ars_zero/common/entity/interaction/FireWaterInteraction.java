package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

public class FireWaterInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return (primary instanceof FireVoxelEntity && secondary instanceof WaterVoxelEntity) ||
               (primary instanceof WaterVoxelEntity && secondary instanceof FireVoxelEntity);
    }
    
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        float primarySize = primary.getSize();
        float secondarySize = secondary.getSize();
        
        VoxelInteractionResult.Builder builder = VoxelInteractionResult.builder(primary.position())
            .particles(ParticleTypes.CLOUD, 30)
            .sound(SoundEvents.FIRE_EXTINGUISH);
        
        if (primarySize > secondarySize) {
            float newSize = primarySize - secondarySize;
            builder.primaryAction(VoxelInteractionResult.ActionType.RESIZE)
                   .primaryNewSize(newSize)
                   .secondaryAction(VoxelInteractionResult.ActionType.DISCARD);
        } else if (secondarySize > primarySize) {
            float newSize = secondarySize - primarySize;
            builder.primaryAction(VoxelInteractionResult.ActionType.DISCARD)
                   .secondaryAction(VoxelInteractionResult.ActionType.RESIZE)
                   .secondaryNewSize(newSize);
        } else {
            builder.primaryAction(VoxelInteractionResult.ActionType.DISCARD)
                   .secondaryAction(VoxelInteractionResult.ActionType.DISCARD);
        }
        
        return builder.build();
    }
}

