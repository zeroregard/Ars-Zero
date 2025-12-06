package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.BlightVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

public class BlightWaterInteraction implements VoxelInteraction {
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        BaseVoxelEntity blight = primary instanceof BlightVoxelEntity ? primary : secondary;
        BaseVoxelEntity water = blight == primary ? secondary : primary;

        float blightSize = blight.getSize();
        float waterSize = water.getSize();
        float neutralized = Math.min(blightSize, waterSize);
        float blightRemaining = Math.max(0.0f, blightSize - (neutralized * 0.6f));
        float waterRemaining = Math.max(0.0f, waterSize - neutralized);

        boolean blightSurvives = blightRemaining >= 0.0625f;
        boolean waterSurvives = waterRemaining >= 0.0625f;

        VoxelInteractionResult.Builder builder = VoxelInteractionResult.builder(primary.position())
            .particles(ParticleTypes.SPLASH, 28)
            .sound(SoundEvents.BREWING_STAND_BREW);

        if (primary instanceof BlightVoxelEntity) {
            builder.primaryAction(blightSurvives ? VoxelInteractionResult.ActionType.RESIZE : VoxelInteractionResult.ActionType.DISCARD);
            if (blightSurvives) {
                builder.primaryNewSize(blightRemaining);
            }
            builder.secondaryAction(waterSurvives ? VoxelInteractionResult.ActionType.RESIZE : VoxelInteractionResult.ActionType.DISCARD);
            if (waterSurvives) {
                builder.secondaryNewSize(waterRemaining);
            }
        } else {
            builder.primaryAction(waterSurvives ? VoxelInteractionResult.ActionType.RESIZE : VoxelInteractionResult.ActionType.DISCARD);
            if (waterSurvives) {
                builder.primaryNewSize(waterRemaining);
            }
            builder.secondaryAction(blightSurvives ? VoxelInteractionResult.ActionType.RESIZE : VoxelInteractionResult.ActionType.DISCARD);
            if (blightSurvives) {
                builder.secondaryNewSize(blightRemaining);
            }
        }

        return builder.build();
    }

    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return (primary instanceof BlightVoxelEntity && secondary instanceof WaterVoxelEntity) ||
               (primary instanceof WaterVoxelEntity && secondary instanceof BlightVoxelEntity);
    }
}
