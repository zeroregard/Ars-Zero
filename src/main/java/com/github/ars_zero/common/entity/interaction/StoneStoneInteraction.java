package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import com.github.ars_zero.common.entity.StoneVoxelEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;

public class StoneStoneInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return primary instanceof StoneVoxelEntity && secondary instanceof StoneVoxelEntity;
    }
    
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return VoxelInteractionResult.builder(primary.position())
            .particles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), 20)
            .primaryAction(VoxelInteractionResult.ActionType.DISCARD)
            .secondaryAction(VoxelInteractionResult.ActionType.DISCARD)
            .build();
    }
}



