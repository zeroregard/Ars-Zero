package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.IceVoxelEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;

public class IceIceInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return primary instanceof IceVoxelEntity && secondary instanceof IceVoxelEntity;
    }
    
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return VoxelInteractionResult.builder(primary.position())
            .particles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), 20)
            .primaryAction(VoxelInteractionResult.ActionType.DISCARD)
            .secondaryAction(VoxelInteractionResult.ActionType.DISCARD)
            .build();
    }
}
