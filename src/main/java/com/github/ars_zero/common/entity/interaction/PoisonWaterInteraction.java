package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.PoisonVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

public class PoisonWaterInteraction implements VoxelInteraction {
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        BaseVoxelEntity poison = primary instanceof PoisonVoxelEntity ? primary : secondary;
        BaseVoxelEntity water = poison == primary ? secondary : primary;

        float poisonSize = poison.getSize();
        float waterSize = water.getSize();
        float neutralized = Math.min(poisonSize, waterSize);
        float poisonRemaining = Math.max(0.0f, poisonSize - (neutralized * 0.6f));
        float waterRemaining = Math.max(0.0f, waterSize - neutralized);

        boolean poisonSurvives = poisonRemaining >= 0.0625f;
        boolean waterSurvives = waterRemaining >= 0.0625f;

        VoxelInteractionResult.Builder builder = VoxelInteractionResult.builder(primary.position())
            .particles(ParticleTypes.SPLASH, 28)
            .sound(SoundEvents.BREWING_STAND_BREW);

        if (primary instanceof PoisonVoxelEntity) {
            builder.primaryAction(poisonSurvives ? VoxelInteractionResult.ActionType.RESIZE : VoxelInteractionResult.ActionType.DISCARD);
            if (poisonSurvives) {
                builder.primaryNewSize(poisonRemaining);
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
            builder.secondaryAction(poisonSurvives ? VoxelInteractionResult.ActionType.RESIZE : VoxelInteractionResult.ActionType.DISCARD);
            if (poisonSurvives) {
                builder.secondaryNewSize(poisonRemaining);
            }
        }

        return builder.build();
    }

    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return (primary instanceof PoisonVoxelEntity && secondary instanceof WaterVoxelEntity) ||
               (primary instanceof WaterVoxelEntity && secondary instanceof PoisonVoxelEntity);
    }
}
