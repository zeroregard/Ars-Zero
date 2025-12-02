package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.IceVoxelEntity;
import com.github.ars_zero.common.entity.StoneVoxelEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;

public class IceStoneInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return (primary instanceof IceVoxelEntity && secondary instanceof StoneVoxelEntity) ||
               (primary instanceof StoneVoxelEntity && secondary instanceof IceVoxelEntity);
    }
    
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        boolean iceIsPrimary = primary instanceof IceVoxelEntity;
        
        return VoxelInteractionResult.builder(primary.position())
            .particles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), 15)
            .primaryAction(iceIsPrimary ? VoxelInteractionResult.ActionType.DISCARD : VoxelInteractionResult.ActionType.CONTINUE)
            .secondaryAction(iceIsPrimary ? VoxelInteractionResult.ActionType.CONTINUE : VoxelInteractionResult.ActionType.DISCARD)
            .build();
    }
}
