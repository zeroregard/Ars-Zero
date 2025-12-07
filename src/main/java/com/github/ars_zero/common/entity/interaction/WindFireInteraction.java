package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.LightningVoxelEntity;
import com.github.ars_zero.common.entity.WindVoxelEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

public class WindFireInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return (primary instanceof WindVoxelEntity && secondary instanceof FireVoxelEntity) ||
               (primary instanceof FireVoxelEntity && secondary instanceof WindVoxelEntity);
    }
    
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        Vec3 position = primary.position();
        float combinedSize = primary.getSize() + secondary.getSize();
        int lifetime = Math.max(primary.getLifetime(), secondary.getLifetime());
        
        if (!primary.level().isClientSide && primary.level() instanceof ServerLevel serverLevel) {
            LightningVoxelEntity lightningVoxel = new LightningVoxelEntity(
                serverLevel,
                position.x,
                position.y,
                position.z,
                lifetime
            );
            lightningVoxel.setSize(combinedSize);
            lightningVoxel.refreshDimensions();
            lightningVoxel.setOwner(primary.getOwner() != null ? primary.getOwner() : secondary.getOwner());
            lightningVoxel.setDeltaMovement(
                (primary.getDeltaMovement().x + secondary.getDeltaMovement().x) / 2.0,
                (primary.getDeltaMovement().y + secondary.getDeltaMovement().y) / 2.0,
                (primary.getDeltaMovement().z + secondary.getDeltaMovement().z) / 2.0
            );
            serverLevel.addFreshEntity(lightningVoxel);
        }
        
        return VoxelInteractionResult.builder(position)
            .particles(ParticleTypes.ELECTRIC_SPARK, 30)
            .particles(ParticleTypes.FLAME, 15)
            .particles(ParticleTypes.CLOUD, 10)
            .sound(SoundEvents.LIGHTNING_BOLT_THUNDER)
            .primaryAction(VoxelInteractionResult.ActionType.DISCARD)
            .secondaryAction(VoxelInteractionResult.ActionType.DISCARD)
            .build();
    }
}


