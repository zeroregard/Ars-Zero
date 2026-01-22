package com.github.ars_zero.common.entity;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.client.particle.GlowParticleData;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.common.block.tile.BasicSpellTurretTile;
import com.hollingsworth.arsnouveau.common.block.tile.CreativeSourceJarTile;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class SourceJarChargerEntity extends AbstractChargerEntity {
  private static final String TAG_JAR_POS = "jar_pos";
  private static final String TAG_SOURCE_ORIGIN_POS = "source_origin_pos";
  private static final String TAG_INITIAL_BURST_DONE = "initial_burst_done";
  private static final String TAG_INITIAL_SOURCE_AMOUNT = "initial_source_amount";

  private static final int DEFAULT_INITIAL_SOURCE_AMOUNT = 200;
  private static final int SOURCE_PER_TICK = 5;
  private static final int SOURCE_DRAIN_RANGE = 10;

  private BlockPos jarPos;
  private BlockPos sourceOriginPos;
  private boolean initialBurstDone = false;
  private int initialSourceAmount = DEFAULT_INITIAL_SOURCE_AMOUNT;

  public SourceJarChargerEntity(net.minecraft.world.entity.EntityType<? extends SourceJarChargerEntity> entityType,
      Level level) {
    super(entityType, level);
  }

  public void setJarPos(BlockPos pos) {
    this.jarPos = pos;
  }

  @Override
  public void tick() {
    super.tick();
  }

  public void setSourceOriginPos(BlockPos pos) {
    this.sourceOriginPos = pos;
  }

  public void setInitialSourceAmount(int amount) {
    this.initialSourceAmount = amount;
  }

  @Override
  protected void onChargeTick(ServerLevel serverLevel) {
    if (jarPos == null) {
      return;
    }
    if (!(serverLevel.getBlockEntity(jarPos) instanceof SourceJarTile jar)) {
      return;
    }
    if (!jar.canAcceptSource()) {
      return;
    }

    int currentSource = jar.getSource();
    int maxSource = jar.getMaxSource();
    int capacity = maxSource - currentSource;
    if (capacity <= 0) {
      return;
    }

    int toAdd;
    if (!initialBurstDone) {
      int freeAmount = Math.min(initialSourceAmount, capacity);
      jar.setSource(currentSource + freeAmount);
      jar.updateBlock();
      initialBurstDone = true;
      spawnChargeParticles(serverLevel, getParticlePosition(), tickCount, 10.0);
      return;
    }

    BlockPos drainPos = sourceOriginPos != null ? sourceOriginPos : this.blockPosition();
    int requested = Math.min(SOURCE_PER_TICK, capacity);
    
    int drained;
    boolean isTurretCaster = isTurretCaster(serverLevel, drainPos);
    if (!isTurretCaster && casterUUID != null && serverLevel.getEntity(casterUUID) instanceof Player casterPlayer) {
      drained = drainFromPlayerMana(serverLevel, casterPlayer, requested);
    } else {
      boolean shouldExcludeJar = shouldExcludeJar(serverLevel, drainPos);
      drained = drainSourceExcludingJar(serverLevel, drainPos, requested, shouldExcludeJar ? jarPos : null);
    }
    
    if (drained <= 0) {
      return;
    }

    toAdd = Math.min(drained, capacity);
    jar.setSource(currentSource + toAdd);
    jar.updateBlock();

    spawnChargeParticles(serverLevel, getParticlePosition(), tickCount, 10.0);
  }

  private int drainFromPlayerMana(ServerLevel serverLevel, Player player, int requested) {
    IManaCap manaCap = CapabilityRegistry.getMana(player);
    if (manaCap == null) {
      return 0;
    }
    
    double currentMana = manaCap.getCurrentMana();
    double manaToDrain = Math.min(requested, currentMana);
    
    if (manaToDrain > 0) {
      manaCap.removeMana(manaToDrain);
      return (int) manaToDrain;
    }
    
    return 0;
  }

  private boolean isTurretCaster(ServerLevel serverLevel, BlockPos originPos) {
    if (originPos == null) {
      return false;
    }
    return serverLevel.getBlockEntity(originPos) instanceof BasicSpellTurretTile;
  }

  private boolean shouldExcludeJar(ServerLevel serverLevel, BlockPos originPos) {
    return isTurretCaster(serverLevel, originPos);
  }

  private int drainSourceExcludingJar(ServerLevel serverLevel, BlockPos originPos, int requested, BlockPos excludeJarPos) {
    List<ISpecialSourceProvider> providers = SourceUtil.canTakeSource(originPos, serverLevel, SOURCE_DRAIN_RANGE);
    if (excludeJarPos != null) {
      providers.removeIf(provider -> {
        BlockPos providerPos = provider.getCurrentPos();
        return providerPos != null && providerPos.equals(excludeJarPos);
      });
    }
    
    if (providers.isEmpty()) {
      return 0;
    }
    
    Multimap<ISpecialSourceProvider, Integer> takenFrom = ArrayListMultimap.create();
    int needed = requested;
    int totalExtracted = 0;
    
    for (ISpecialSourceProvider provider : providers) {
      ISourceTile sourceTile = provider.getSource();
      if (sourceTile instanceof CreativeSourceJarTile) {
        for (var entry : takenFrom.entries()) {
          entry.getKey().getSource().addSource(entry.getValue());
        }
        int extracted = Math.min(needed, sourceTile.getSource());
        sourceTile.removeSource(extracted);
        return totalExtracted + extracted;
      }
      
      if (needed <= 0) {
        continue;
      }
      
      int initial = sourceTile.getSource();
      int available = Math.min(needed, initial);
      int after = sourceTile.removeSource(available);
      if (initial > after) {
        int extracted = initial - after;
        needed -= extracted;
        totalExtracted += extracted;
        takenFrom.put(provider, extracted);
      }
    }
    
    if (needed > 0) {
      for (var entry : takenFrom.entries()) {
        entry.getKey().getSource().addSource(entry.getValue());
      }
      return 0;
    }
    
    return totalExtracted;
  }

  @Override
  protected boolean canTransferMana() {
    if (jarPos == null || this.level().isClientSide) {
      return false;
    }
    if (this.level().getBlockEntity(jarPos) instanceof SourceJarTile jar) {
      return jar.canAcceptSource();
    }
    return false;
  }

  @Override
  protected boolean transferMana(ServerLevel level, int manaAmount) {
    if (jarPos == null) {
      return false;
    }
    if (level.getBlockEntity(jarPos) instanceof SourceJarTile jar) {
      int currentSource = jar.getSource();
      int maxSource = jar.getMaxSource();
      int sourceToAdd = Math.min(manaAmount, maxSource - currentSource);
      if (sourceToAdd > 0) {
        jar.setSource(currentSource + sourceToAdd);
        jar.updateBlock();
        return true;
      }
    }
    return false;
  }

  @Override
  protected Vec3 getParticlePosition() {
    if (jarPos != null) {
      return Vec3.atCenterOf(jarPos);
    }
    return this.position();
  }

  @Override
  protected void spawnChargeParticles(ServerLevel level, Vec3 position, int tickCount, double manaRegen) {
    double centerX = position.x;
    double centerY = position.y;
    double centerZ = position.z;

    double orbitRadius = 0.9;
    double baseRotationSpeed = 0.15;
    double regenMultiplier = Math.max(0.5, Math.min(2.0, manaRegen / 10.0));
    double rotationSpeed = baseRotationSpeed * regenMultiplier;

    ParticleColor purpleColor = ParticleColor.PURPLE;

    int particleCount = 3;
    for (int i = 0; i < particleCount; i++) {
      double particleOffset = i * 2.0 * Math.PI / particleCount;
      double baseAngle = (tickCount * rotationSpeed) + particleOffset;

      double heightOffset = Math.sin(tickCount * 0.1 + i * 0.7) * 0.2;

      double x = centerX + Math.cos(baseAngle) * orbitRadius;
      double y = centerY + heightOffset;
      double z = centerZ + Math.sin(baseAngle) * orbitRadius;

      double baseOrbitSpeed = 0.1;
      double orbitSpeed = baseOrbitSpeed * regenMultiplier;
      double speedX = -Math.sin(baseAngle) * orbitSpeed;
      double speedY = Math.cos(tickCount * 0.1 + i * 0.7) * 0.02;
      double speedZ = Math.cos(baseAngle) * orbitSpeed;

      double toCenterX = centerX - x;
      double toCenterY = centerY - y;
      double toCenterZ = centerZ - z;
      double distToCenter = Math.sqrt(toCenterX * toCenterX + toCenterZ * toCenterZ);

      if (distToCenter > 0.05) {
        double inwardPull = 0.03;
        speedX += (toCenterX / distToCenter) * inwardPull;
        speedZ += (toCenterZ / distToCenter) * inwardPull;
      }

      level.sendParticles(GlowParticleData.createData(purpleColor), x, y, z, 1, speedX, speedY, speedZ, 0.0);
    }
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag compound) {
    super.readAdditionalSaveData(compound);
    if (compound.contains(TAG_JAR_POS)) {
      this.jarPos = BlockPos.of(compound.getLong(TAG_JAR_POS));
    }
    if (compound.contains(TAG_SOURCE_ORIGIN_POS)) {
      this.sourceOriginPos = BlockPos.of(compound.getLong(TAG_SOURCE_ORIGIN_POS));
    }
    if (compound.contains(TAG_INITIAL_BURST_DONE)) {
      this.initialBurstDone = compound.getBoolean(TAG_INITIAL_BURST_DONE);
    } else if (compound.contains("initial_source_added")) {
      this.initialBurstDone = compound.getBoolean("initial_source_added");
    }
    if (compound.contains(TAG_INITIAL_SOURCE_AMOUNT)) {
      this.initialSourceAmount = compound.getInt(TAG_INITIAL_SOURCE_AMOUNT);
    }
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag compound) {
    super.addAdditionalSaveData(compound);
    if (jarPos != null) {
      compound.putLong(TAG_JAR_POS, jarPos.asLong());
    }
    if (sourceOriginPos != null) {
      compound.putLong(TAG_SOURCE_ORIGIN_POS, sourceOriginPos.asLong());
    }
    compound.putBoolean(TAG_INITIAL_BURST_DONE, initialBurstDone);
    compound.putInt(TAG_INITIAL_SOURCE_AMOUNT, initialSourceAmount);
  }
}
