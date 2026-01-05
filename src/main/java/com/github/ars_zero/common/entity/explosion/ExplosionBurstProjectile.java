package com.github.ars_zero.common.entity.explosion;

import com.hollingsworth.arsnouveau.api.ANFakePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ExplosionBurstProjectile extends Projectile {

  private static final EntityDataAccessor<Float> COLOR_R = SynchedEntityData.defineId(ExplosionBurstProjectile.class,
      EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Float> COLOR_G = SynchedEntityData.defineId(ExplosionBurstProjectile.class,
      EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Float> COLOR_B = SynchedEntityData.defineId(ExplosionBurstProjectile.class,
      EntityDataSerializers.FLOAT);

  private int age = 0;
  private static final int MAX_AGE = 100; // 5 seconds max lifetime
  private static final double GRAVITY = 0.08; // Stronger gravity than default
  @Nullable
  private UUID casterUUID = null;

  public ExplosionBurstProjectile(EntityType<? extends Projectile> entityType, Level level) {
    super(entityType, level);
    this.setNoGravity(true); // We handle gravity manually
  }

  public ExplosionBurstProjectile(EntityType<? extends Projectile> entityType, Level level, double x, double y,
      double z, double dx, double dy, double dz, float r, float g, float b) {
    this(entityType, level);
    this.setPos(x, y, z);
    this.setDeltaMovement(dx, dy, dz);
    this.setColor(r, g, b);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    builder.define(COLOR_R, 1.0f);
    builder.define(COLOR_G, 0.5f);
    builder.define(COLOR_B, 0.0f);
  }

  public void setColor(float r, float g, float b) {
    this.entityData.set(COLOR_R, r);
    this.entityData.set(COLOR_G, g);
    this.entityData.set(COLOR_B, b);
  }

  public float getR() {
    return this.entityData.get(COLOR_R);
  }

  public float getG() {
    return this.entityData.get(COLOR_G);
  }

  public float getB() {
    return this.entityData.get(COLOR_B);
  }

  public boolean isSoulfire() {
    // Blue color indicates soulfire (0.3, 0.7, 1.0)
    return this.getB() > 0.9f && this.getR() < 0.5f;
  }

  public void setCasterUUID(@Nullable UUID casterUUID) {
    this.casterUUID = casterUUID;
  }

  @Nullable
  public UUID getCasterUUID() {
    return casterUUID;
  }

  @Nullable
  private Player getClaimActor(ServerLevel level) {
    if (casterUUID == null) {
      return null;
    }
    if (level.getServer() != null && level.getServer().getPlayerList() != null) {
      Player realPlayer = level.getServer().getPlayerList().getPlayer(casterUUID);
      if (realPlayer != null) {
        return realPlayer;
      }
    }
    return ANFakePlayer.getPlayer(level, casterUUID);
  }

  @Override
  public void tick() {
    super.tick();
    this.age++;

    if (this.level().isClientSide) {

      this.level().addParticle(
          ParticleTypes.CAMPFIRE_COSY_SMOKE,
          this.xo, this.yo, this.zo,
          (this.level().getRandom().nextDouble() - 0.5) * 0.01,
          (this.level().getRandom().nextDouble() - 0.5) * 0.01,
          (this.level().getRandom().nextDouble() - 0.5) * 0.01);

    }

    // Apply gravity and air resistance
    Vec3 motion = this.getDeltaMovement();
    Vec3 newMotion = motion.multiply(0.96, 0.96, 0.96);
    newMotion = newMotion.add(0, -GRAVITY, 0);
    this.setDeltaMovement(newMotion);

    Vec3 thisPos = this.position();
    Vec3 nextPos = thisPos.add(this.getDeltaMovement());

    // Check for block hits
    HitResult blockHitResult = this.level().clip(new ClipContext(
        thisPos, nextPos,
        ClipContext.Block.COLLIDER,
        ClipContext.Fluid.NONE,
        this));

    if (blockHitResult.getType() != HitResult.Type.MISS) {
      nextPos = blockHitResult.getLocation();
    }

    // Check for entity hits
    EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
        this.level(),
        this,
        thisPos,
        nextPos,
        this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0),
        (entity) -> !entity.isSpectator() && entity.isPickable());

    HitResult hitResult = blockHitResult;
    if (entityHitResult != null) {
      hitResult = entityHitResult;
    }

    if (hitResult.getType() != HitResult.Type.MISS
        && !net.neoforged.neoforge.event.EventHooks.onProjectileImpact(this, hitResult)) {
      this.onHit(hitResult);
      this.hasImpulse = true;
      this.discard();
      return;
    }

    // Move the projectile
    Vec3 deltaMovement = this.getDeltaMovement();
    this.setPos(this.getX() + deltaMovement.x, this.getY() + deltaMovement.y, this.getZ() + deltaMovement.z);

    // Remove if too old
    if (this.age >= MAX_AGE) {
      this.discard();
    }
  }

  @Override
  protected void onHitBlock(BlockHitResult result) {
    super.onHitBlock(result);
    if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
      Player claimActor = getClaimActor(serverLevel);
      boolean isSoulfire = this.isSoulfire();
      FireIgnitionHelper.igniteBlock(serverLevel, result.getBlockPos(), isSoulfire, claimActor);
    }
  }

  @Override
  protected void onHitEntity(EntityHitResult result) {
    super.onHitEntity(result);
    if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity living) {
      int fireTicks = 100; // 5 seconds of fire
      FireIgnitionHelper.igniteEntity(living, fireTicks);
    }
  }

  @Override
  protected void onHit(HitResult result) {
    if (result instanceof BlockHitResult blockHit) {
      this.onHitBlock(blockHit);
    } else if (result instanceof EntityHitResult entityHit) {
      this.onHitEntity(entityHit);
    }
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag compound) {
    super.readAdditionalSaveData(compound);
    if (compound.contains("casterUUID")) {
      this.casterUUID = compound.getUUID("casterUUID");
    }
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag compound) {
    super.addAdditionalSaveData(compound);
    if (casterUUID != null) {
      compound.putUUID("casterUUID", casterUUID);
    }
  }
}
