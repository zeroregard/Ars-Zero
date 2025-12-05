package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class PoisonVoxelEntity extends BaseVoxelEntity {
    private static final int COLOR = 0x6BFF78;
    private static final ResourceKey<net.minecraft.world.effect.MobEffect> ENVENOM_EFFECT_KEY =
        ResourceKey.create(Registries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath("ars_nouveau", "envenom"));
    private static final ResourceKey<net.minecraft.world.effect.MobEffect> POISON_EFFECT_KEY =
        ResourceKey.create(Registries.MOB_EFFECT, ResourceLocation.parse("minecraft:poison"));
    private static final int LINGER_INTERVAL = 15;
    private static final double BASE_CLOUD_RADIUS = 1.5D;

    public PoisonVoxelEntity(EntityType<? extends PoisonVoxelEntity> entityType, Level level) {
        super(entityType, level);
    }

    public PoisonVoxelEntity(Level level, double x, double y, double z, int lifetime) {
        this(ModEntities.POISON_VOXEL_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.setLifetime(lifetime);
    }

    @Override
    public int getColor() {
        return COLOR;
    }

    @Override
    public boolean isEmissive() {
        return false;
    }

    @Override
    protected ParticleOptions getAmbientParticle() {
        return ParticleTypes.ITEM_SLIME;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.isAlive() && this.age % LINGER_INTERVAL == 0) {
            releaseCloud(false);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof BaseVoxelEntity) {
            super.onHitEntity(result);
            return;
        }
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity living) {
            applyEffect(living, true);
        }
        spawnHitParticles(result.getLocation());
        releaseCloud(true);
        this.discard();
    }

    @Override
    protected void onBlockCollision(BlockHitResult blockHit) {
        releaseCloud(true);
    }

    @Override
    protected void spawnHitParticles(Vec3 location) {
        spawnCloudParticles(location, 32);
    }

    private void releaseCloud(boolean burst) {
        if (this.level().isClientSide) {
            return;
        }
        Vec3 center = this.position();
        double sizeScale = Math.max(1.0D, this.getSize() / BaseVoxelEntity.DEFAULT_BASE_SIZE);
        double radius = BASE_CLOUD_RADIUS * sizeScale * (burst ? 1.25D : 1.0D);
        AABB area = AABB.ofSize(center, radius * 2.0D, radius * 2.0D, radius * 2.0D);
        this.level().getEntitiesOfClass(LivingEntity.class, area, entity -> entity.isAlive() && !entity.isSpectator())
            .forEach(living -> {
                double distanceSq = living.distanceToSqr(center.x, center.y, center.z);
                if (distanceSq <= radius * radius) {
                    applyEffect(living, burst);
                }
            });
        spawnCloudParticles(center, burst ? 48 : 20);
    }

    private void spawnCloudParticles(Vec3 center, int count) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double spread = Math.max(0.2D, this.getSize());
        for (int i = 0; i < count; i++) {
            double ox = (this.random.nextDouble() - 0.5D) * spread;
            double oy = (this.random.nextDouble() - 0.5D) * spread;
            double oz = (this.random.nextDouble() - 0.5D) * spread;
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, center.x + ox, center.y + oy, center.z + oz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void applyEffect(LivingEntity living, boolean burst) {
        Holder<net.minecraft.world.effect.MobEffect> effect = resolveEnvenomEffect();
        int duration = getEffectDuration(burst);
        int amplifier = getEffectAmplifier(burst);
        living.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
    }

    private Holder<net.minecraft.world.effect.MobEffect> resolveEnvenomEffect() {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ENVENOM_EFFECT_KEY)
            .or(() -> BuiltInRegistries.MOB_EFFECT.getHolder(POISON_EFFECT_KEY))
            .orElseThrow();
    }

    private int getEffectDuration(boolean burst) {
        float sizeScale = Math.max(1.0f, this.getSize() / BaseVoxelEntity.DEFAULT_BASE_SIZE);
        int base = burst ? 160 : 100;
        return (int) (base * sizeScale);
    }

    private int getEffectAmplifier(boolean burst) {
        float sizeScale = this.getSize() / BaseVoxelEntity.DEFAULT_BASE_SIZE;
        if (sizeScale >= 3.0f) {
            return burst ? 2 : 1;
        }
        if (sizeScale >= 2.0f) {
            return burst ? 1 : 0;
        }
        return 0;
    }
}
