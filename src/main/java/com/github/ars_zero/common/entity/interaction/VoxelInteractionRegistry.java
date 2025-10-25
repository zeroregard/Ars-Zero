package com.github.ars_zero.common.entity.interaction;

import com.github.ars_zero.common.entity.BaseVoxelEntity;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

public class VoxelInteractionRegistry {
    
    private static final Map<Pair<Class<? extends BaseVoxelEntity>, Class<? extends BaseVoxelEntity>>, VoxelInteraction> interactions = new HashMap<>();
    
    public static void register(Class<? extends BaseVoxelEntity> type1, Class<? extends BaseVoxelEntity> type2, VoxelInteraction interaction) {
        interactions.put(Pair.of(type1, type2), interaction);
        interactions.put(Pair.of(type2, type1), interaction);
    }
    
    public static VoxelInteraction getInteraction(BaseVoxelEntity voxel1, BaseVoxelEntity voxel2) {
        return interactions.get(Pair.of(voxel1.getClass(), voxel2.getClass()));
    }
    
    public static void clear() {
        interactions.clear();
    }
}


