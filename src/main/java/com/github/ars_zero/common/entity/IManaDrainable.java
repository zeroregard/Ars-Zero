package com.github.ars_zero.common.entity;

import com.github.ars_zero.common.network.PacketManaDrain;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public interface IManaDrainable {
  int DRAIN_SYNC_INTERVAL = 20;

  double getManaCostPerBlock();

  UUID getCasterUUID();

  double getAccumulatedDrain();

  void setAccumulatedDrain(double value);

  int getTicksSinceLastDrainSync();

  void setTicksSinceLastDrainSync(int value);

  Player getCasterPlayer();

  void setCasterPlayer(Player player);

  default boolean consumeManaAndAccumulate(ServerLevel level, double manaCost) {
    Player player = getCasterPlayer();
    if (player == null) {
      if (this instanceof Entity entityInstance) {
        entityInstance.discard();
      }
      return false;
    }

    boolean isCreative = player.getAbilities().instabuild;
    IManaCap manaCap = CapabilityRegistry.getMana(player);

    if (manaCap == null) {
      return isCreative;
    }

    if (manaCap.getCurrentMana() >= manaCost) {
      manaCap.removeMana(manaCost);
      setAccumulatedDrain(getAccumulatedDrain() + manaCost);
      return true;
    }

    return isCreative;
  }

  default void tickAndSyncDrain(ServerLevel level) {
    int ticks = getTicksSinceLastDrainSync() + 1;
    setTicksSinceLastDrainSync(ticks);

    if (ticks >= DRAIN_SYNC_INTERVAL && getAccumulatedDrain() > 0) {
      sendDrainPacket(level, getAccumulatedDrain());
      setAccumulatedDrain(0);
      setTicksSinceLastDrainSync(0);
    }
  }

  default void sendDrainPacket(ServerLevel level, double amount) {
    Player player = getCasterPlayer();
    if (player instanceof ServerPlayer serverPlayer) {
      PacketDistributor.sendToPlayer(serverPlayer, new PacketManaDrain(amount));
    }
  }
}
