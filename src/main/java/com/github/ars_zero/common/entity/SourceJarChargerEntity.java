package com.github.ars_zero.common.entity;

import com.hollingsworth.arsnouveau.client.particle.GlowParticleData;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SourceJarChargerEntity extends AbstractChargerEntity {
  private static final String TAG_JAR_POS = "jar_pos";
  private static final String TAG_SOURCE_ORIGIN_POS = "source_origin_pos";
  private static final String TAG_INITIAL_BURST_DONE = "initial_burst_done";

  private static final int INITIAL_SOURCE_AMOUNT = 200;
  private static final int SOURCE_PER_TICK = 5;
  private static final int SOURCE_DRAIN_RANGE = 10;

  private BlockPos jarPos;
  private BlockPos sourceOriginPos;
  private boolean initialBurstDone = false;

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

    BlockPos drainPos = sourceOriginPos != null ? sourceOriginPos : this.blockPosition();
    int requested = initialBurstDone ? Math.min(SOURCE_PER_TICK, capacity) : Math.min(INITIAL_SOURCE_AMOUNT, capacity);
    int drained = drainSource(serverLevel, drainPos, requested);
    if (drained <= 0) {
      return;
    }

    int toAdd = Math.min(drained, maxSource - currentSource);
    if (toAdd <= 0) {
      return;
    }

    jar.setSource(currentSource + toAdd);
    jar.updateBlock();

    initialBurstDone = true;
    spawnChargeParticles(serverLevel, getParticlePosition(), tickCount, 10.0);
  }

  private static int drainSource(ServerLevel serverLevel, BlockPos originPos, int requested) {
    int amount = requested;
    while (amount > 0) {
      if (SourceUtil.takeSourceMultipleWithParticles(originPos, serverLevel, SOURCE_DRAIN_RANGE, amount) != null) {
        return amount;
      }
      amount /= 2;
    }
    return 0;
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
  }
}
