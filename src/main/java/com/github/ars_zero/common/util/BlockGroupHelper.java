package com.github.ars_zero.common.util;

import com.github.ars_zero.common.entity.BlockGroupEntity;
import com.github.ars_zero.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class BlockGroupHelper {
    private BlockGroupHelper() {
    }

    public static boolean isBlockBreakable(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        return isBlockBreakable(state, level, pos);
    }

    public static boolean isBlockBreakable(@Nullable BlockState state, Level level, BlockPos pos) {
        if (state == null || state.isAir()) {
            return false;
        }

        return state.getDestroySpeed(level, pos) >= 0.0f;
    }

    public static BlockGroupEntity spawnBlockGroup(ServerLevel level, Player caster, List<BlockPos> positions, boolean ghostMode, boolean removeOriginalBlocks, @Nullable Map<BlockPos, BlockState> capturedStates) {
        if (positions.isEmpty()) {
            throw new IllegalArgumentException("Block group positions cannot be empty.");
        }

        Vec3 centerPos = calculateCenter(positions);
        BlockGroupEntity blockGroup = new BlockGroupEntity(ModEntities.BLOCK_GROUP.get(), level);
        blockGroup.setPos(centerPos.x, centerPos.y, centerPos.z);
        blockGroup.setCasterUUID(caster.getUUID());
        blockGroup.setGhostMode(ghostMode);

        if (capturedStates != null) {
            blockGroup.addBlocksWithStates(positions, capturedStates);
        } else {
            blockGroup.addBlocks(positions);
        }

        if (removeOriginalBlocks) {
            blockGroup.removeOriginalBlocks();
        }

        level.addFreshEntity(blockGroup);
        return blockGroup;
    }

    private static Vec3 calculateCenter(List<BlockPos> positions) {
        double x = 0;
        double y = 0;
        double z = 0;

        for (BlockPos pos : positions) {
            x += pos.getX() + 0.5;
            y += pos.getY() + 0.5;
            z += pos.getZ() + 0.5;
        }

        int count = positions.size();
        return new Vec3(x / count, y / count, z / count);
    }
}
