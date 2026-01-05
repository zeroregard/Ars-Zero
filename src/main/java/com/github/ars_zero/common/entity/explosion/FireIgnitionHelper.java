package com.github.ars_zero.common.entity.explosion;

import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FireIgnitionHelper {

  private static final int UPDATE_FLAGS = net.minecraft.world.level.block.Block.UPDATE_CLIENTS;
  private static final double SOULFIRE_THRESHOLD = 8.0;

  /**
   * Attempts to ignite a block at the given position.
   * If isSoulfire is true, attempts to place soul fire first, then falls back to
   * regular fire.
   * Also handles special cases like converting blocks to soul sand for low
   * hardness blocks.
   * 
   * @param level      The server level
   * @param hitPos     The position that was hit (usually a solid block)
   * @param isSoulfire Whether to use soulfire (blue) or regular fire (orange)
   * @param claimActor The player to check claims for, or null to skip claim
   *                   checks
   * @return true if fire was placed, false otherwise
   */
  public static boolean igniteBlock(ServerLevel level, BlockPos hitPos, boolean isSoulfire,
      @Nullable Player claimActor) {
    BlockState state = level.getBlockState(hitPos);

    // Try to place fire above the hit block
    BlockPos firePos = hitPos.above();

    if (level.isOutsideBuildHeight(firePos)) {
      return false;
    }

    BlockState firePosState = level.getBlockState(firePos);
    if (!firePosState.isAir()) {
      return false;
    }

    if (claimActor != null) {
      if (!BlockUtil.destroyRespectsClaim(claimActor, level, hitPos) ||
          !BlockUtil.destroyRespectsClaim(claimActor, level, firePos)) {
        return false;
      }
    }

    // For soulfire, use regular fire if block is too soft (hardness < 0.5)
    // This prevents leaves and other soft blocks from being converted to soul sand
    boolean useSoulfireForThisBlock = isSoulfire && !hasLowHardness(level, hitPos, state);

    // For soulfire on blocks with sufficient hardness, convert to soul sand
    if (useSoulfireForThisBlock) {
      float hardness = state.getDestroySpeed(level, hitPos);
      if (hardness >= 0.5f && hardness < 2.0f) { // Only convert medium hardness blocks
        level.setBlock(hitPos, Blocks.SOUL_SAND.defaultBlockState(), UPDATE_FLAGS);
      }
    }

    BlockState fireState = useSoulfireForThisBlock ? Blocks.SOUL_FIRE.defaultBlockState()
        : Blocks.FIRE.defaultBlockState();

    if (fireState.canSurvive(level, firePos)) {
      level.setBlock(firePos, fireState, UPDATE_FLAGS);
      return true;
    } else if (useSoulfireForThisBlock) {
      // Fallback to regular fire if soul fire can't survive
      BlockState regularFireState = Blocks.FIRE.defaultBlockState();
      if (regularFireState.canSurvive(level, firePos)) {
        level.setBlock(firePos, regularFireState, UPDATE_FLAGS);
        return true;
      }
    }

    return false;
  }

  /**
   * Ignores a living entity by setting it on fire.
   * 
   * @param entity    The living entity to ignite
   * @param fireTicks The number of ticks to set the entity on fire for
   */
  public static void igniteEntity(LivingEntity entity, int fireTicks) {
    entity.setRemainingFireTicks(entity.getRemainingFireTicks() + fireTicks);
  }

  /**
   * Determines if soulfire should be used based on fire power.
   */
  public static boolean shouldUseSoulfire(double firePower) {
    return firePower >= SOULFIRE_THRESHOLD;
  }

  private static boolean hasLowHardness(ServerLevel level, BlockPos pos, BlockState state) {
    return state.getDestroySpeed(level, pos) < 0.5f;
  }
}
