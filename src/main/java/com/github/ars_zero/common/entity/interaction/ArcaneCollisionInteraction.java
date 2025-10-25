package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.ArcaneVoxelEntity;
import com.github.ars_zero.common.entity.BaseVoxelEntity;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;

public class ArcaneCollisionInteraction implements VoxelInteraction {
    
    @Override
    public boolean shouldInteract(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        return primary instanceof ArcaneVoxelEntity || secondary instanceof ArcaneVoxelEntity;
    }
    
    @Override
    public VoxelInteractionResult interact(BaseVoxelEntity primary, BaseVoxelEntity secondary) {
        Vector3f color = new Vector3f(0.54f, 0.17f, 0.89f);
        
        return VoxelInteractionResult.builder(primary.position())
            .primaryAction(VoxelInteractionResult.ActionType.RESOLVE)
            .secondaryAction(VoxelInteractionResult.ActionType.RESOLVE)
            .particles(new DustParticleOptions(color, 1.0f), 20)
            .build();
    }
}

