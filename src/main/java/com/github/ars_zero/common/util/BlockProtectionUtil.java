package com.github.ars_zero.common.util;

import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BlockProtectionUtil {

  public static boolean canBlockBeDestroyed(ServerLevel level, BlockPos pos, @Nullable Player claimActor) {
    if (level.isOutsideBuildHeight(pos)) {
      return false;
    }

    BlockState state = level.getBlockState(pos);

    if (BlockImmutabilityUtil.isBlockImmutable(state)) {
      return false;
    }

    if (state.getDestroySpeed(level, pos) < 0.0f) {
      return false;
    }

    if (claimActor != null && !BlockUtil.destroyRespectsClaim(claimActor, level, pos)) {
      return false;
    }

    if (!checkBlockBreakEvent(level, pos, claimActor)) {
      return false;
    }

    return true;
  }

  private static boolean checkBlockBreakEvent(ServerLevel level, BlockPos pos, @Nullable Player claimActor) {
    BlockState state = level.getBlockState(pos);

    Player testPlayer = claimActor;
    if (testPlayer == null) {
      testPlayer = createTestPlayer(level);
      if (testPlayer == null) {
        return true;
      }
    } else if (testPlayer instanceof FakePlayer) {
      Player realPlayer = tryGetRealPlayer(level, testPlayer.getUUID());
      if (realPlayer != null) {
        testPlayer = realPlayer;
      }
    }

    BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, state, testPlayer);
    NeoForge.EVENT_BUS.post(breakEvent);

    return !breakEvent.isCanceled();
  }

  @Nullable
  private static Player tryGetRealPlayer(ServerLevel level, UUID playerUUID) {
    if (level.getServer() == null || level.getServer().getPlayerList() == null) {
      return null;
    }

    return level.getServer().getPlayerList().getPlayer(playerUUID);
  }

  @Nullable
  private static Player createTestPlayer(ServerLevel level) {
    if (level.getServer() == null || level.getServer().getPlayerList() == null) {
      return null;
    }

    var players = level.getServer().getPlayerList().getPlayers();
    if (players.isEmpty()) {
      return null;
    }

    return players.get(0);
  }
}
