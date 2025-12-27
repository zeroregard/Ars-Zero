package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.BlightVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

public class BlightWaterInteraction implements VoxelInteraction {
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        VoxelInteractionResult.Builder builder = VoxelInteractionResult.builder(primary.position())
            .particles(ParticleTypes.SPLASH, 28)
            .sound(SoundEvents.BREWING_STAND_BREW);

        if (primary instanceof BlightVoxelEntity) {
            builder.primaryAction(VoxelInteractionResult.ActionType.DISCARD)
                   .secondaryAction(VoxelInteractionResult.ActionType.CONTINUE);
        } else {
            builder.primaryAction(VoxelInteractionResult.ActionType.CONTINUE)
                   .secondaryAction(VoxelInteractionResult.ActionType.DISCARD);
        }

        return builder.build();
    }

    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return (primary instanceof BlightVoxelEntity && secondary instanceof WaterVoxelEntity) ||
               (primary instanceof WaterVoxelEntity && secondary instanceof BlightVoxelEntity);
    }
}

