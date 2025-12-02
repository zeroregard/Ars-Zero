package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.FireVoxelEntity;
import com.github.ars_zero.common.entity.IceVoxelEntity;
import com.github.ars_zero.common.entity.WaterVoxelEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
        float combinedSize = (primary.getSize() + secondary.getSize()) / 2.0f;
        int lifetime = Math.max(primary.getLifetime(), secondary.getLifetime());
        
        if (!primary.level().isClientSide && primary.level() instanceof ServerLevel serverLevel) {
            WaterVoxelEntity waterVoxel = new WaterVoxelEntity(
                serverLevel,
                position.x,
                position.y,
                position.z,
                lifetime
            );
            waterVoxel.setSize(combinedSize);
            waterVoxel.refreshDimensions();
            waterVoxel.setOwner(primary.getOwner() != null ? primary.getOwner() : secondary.getOwner());
            waterVoxel.setDeltaMovement(
                (primary.getDeltaMovement().x + secondary.getDeltaMovement().x) / 2.0,
                (primary.getDeltaMovement().y + secondary.getDeltaMovement().y) / 2.0,
                (primary.getDeltaMovement().z + secondary.getDeltaMovement().z) / 2.0
            );
            serverLevel.addFreshEntity(waterVoxel);
        }
        
        return VoxelInteractionResult.builder(position)
            .particles(ParticleTypes.CLOUD, 20)
            .sound(SoundEvents.FIRE_EXTINGUISH)
            .primaryAction(VoxelInteractionResult.ActionType.DISCARD)
            .secondaryAction(VoxelInteractionResult.ActionType.DISCARD)
            .build();
    }
}
