package com.github.ars_zero.common.entity;

import com.github.ars_zero.common.glyph.EffectBeam;
import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import org.jetbrains.annotations.Nullable;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public class EffectBeamEntity extends Entity implements ILifespanExtendable, IManaDrainable {

    public static final int DEFAULT_LIFETIME_TICKS = 5;
    private static final double RAY_LENGTH = 300.0;
    private static final float BASE_DAMAGE = 2.0f;

    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAX_LIFETIME = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> COLOR_R = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_G = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_B = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DAMPENED = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID casterUUID;
    @Nullable
    protected SpellResolver resolver;

    private double forwardedSpellManaCost = 0.0;
    private double accumulatedDrain = 0.0;
    private int ticksSinceLastDrainSync = 0;

    public EffectBeamEntity(EntityType<? extends EffectBeamEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.noCulling = true;
        this.setBoundingBox(new AABB(0, 0, 0, 0, 0, 0));
    }

    public EffectBeamEntity(Level level, double x, double y, double z, float yRot, float xRot, int lifetime, float r, float g, float b, @Nullable UUID casterUUID, boolean dampened) {
        this(ModEntities.EFFECT_BEAM.get(), level);
        this.setPos(x, y, z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.setLifetime(lifetime);
        this.setMaxLifetime(lifetime);
        this.setColor(r, g, b);
        this.casterUUID = casterUUID;
        this.setDampened(dampened);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LIFETIME, DEFAULT_LIFETIME_TICKS);
        builder.define(MAX_LIFETIME, DEFAULT_LIFETIME_TICKS);
        builder.define(COLOR_R, 1.0f);
        builder.define(COLOR_G, 1.0f);
        builder.define(COLOR_B, 1.0f);
        builder.define(DAMPENED, false);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(LIFETIME, lifetime);
    }

    public int getMaxLifetime() {
        return this.entityData.get(MAX_LIFETIME);
    }

    public void setMaxLifetime(int maxLifetime) {
        this.entityData.set(MAX_LIFETIME, maxLifetime);
    }

    public void setColor(float r, float g, float b) {
        this.entityData.set(COLOR_R, r);
        this.entityData.set(COLOR_G, g);
        this.entityData.set(COLOR_B, b);
    }

    public float getColorR() {
        return this.entityData.get(COLOR_R);
    }

    public float getColorG() {
        return this.entityData.get(COLOR_G);
    }

    public float getColorB() {
        return this.entityData.get(COLOR_B);
    }

    public boolean isDampened() {
        return this.entityData.get(DAMPENED);
    }

    public void setDampened(boolean dampened) {
        this.entityData.set(DAMPENED, dampened);
    }

    public void setResolver(@Nullable SpellResolver resolver) {
        this.resolver = resolver;
        if (resolver != null && resolver.spellContext != null) {
            forwardedSpellManaCost = calculateForwardedSpellManaCost(resolver.spellContext);
        } else {
            forwardedSpellManaCost = 0.0;
        }
    }

    private double calculateForwardedSpellManaCost(SpellContext context) {
        if (context == null || context.getSpell() == null) {
            return 0.0;
        }
        List<AbstractSpellPart> recipe = context.getSpell().unsafeList();
        if (recipe.isEmpty()) {
            return 0.0;
        }
        int start = 0;
        while (start < recipe.size() && recipe.get(start) instanceof AbstractAugment) {
            start++;
        }
        if (start >= recipe.size()) {
            return 0.0;
        }
        List<AbstractSpellPart> forwardedParts = recipe.subList(start, recipe.size());
        boolean hasEffect = false;
        for (AbstractSpellPart part : forwardedParts) {
            if (part instanceof AbstractEffect) {
                hasEffect = true;
                break;
            }
        }
        if (!hasEffect) {
            return 0.0;
        }
        Spell forwardedSpell = new Spell(forwardedParts);
        int totalCost = forwardedSpell.getCost();
        double multiplier = EffectBeam.INSTANCE.getResolverManaCostMultiplier();
        return totalCost * multiplier;
    }

    @Nullable
    public SpellResolver getResolver() {
        return this.resolver;
    }

    public Vec3 getForward() {
        return Vec3.directionFromRotation(this.getXRot(), this.getYRot());
    }

    @Override
    public void addLifespan(LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        int extensionAmount = 1;
        if (spellStats != null) {
            int extendTimeLevel = spellStats.getBuffCount(AugmentExtendTime.INSTANCE);
            extensionAmount += extendTimeLevel;
        }
        int maxLifetime = this.getMaxLifetime();
        this.setLifetime(Math.min(this.getLifetime() + extensionAmount, maxLifetime));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            tickAndSyncDrain(serverLevel);
            if (this.getLifetime() <= 0) {
                this.discard();
                return;
            }
            this.setLifetime(this.getLifetime() - 1);
        }

        Vec3 origin = this.position();
        Vec3 forward = this.getForward();
        Vec3 end = origin.add(forward.scale(RAY_LENGTH));

        BlockHitResult blockHit = this.level().clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(this.level(), this, origin, end, this.getBoundingBox().inflate(RAY_LENGTH), e -> e.isPickable() && !e.isSpectator() && e != this);

        HitResult hitResult = blockHit;
        if (entityHit != null) {
            double entityDist = origin.distanceTo(entityHit.getLocation());
            double blockDist = blockHit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : origin.distanceTo(blockHit.getLocation());
            if (entityDist < blockDist) {
                hitResult = entityHit;
            }
        }

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            DustParticleOptions particleOptions = new DustParticleOptions(new Vector3f(this.getColorR(), this.getColorG(), this.getColorB()), 0.8f);
            // serverLevel.sendParticles(particleOptions, origin.x, origin.y, origin.z, 2, 0.02, 0.02, 0.02, 0.0);

            if (hitResult.getType() != HitResult.Type.MISS) {
                boolean hitIsCaster = hitResult instanceof EntityHitResult entityHitResult
                    && casterUUID != null
                    && entityHitResult.getEntity().getUUID().equals(casterUUID);
                if (hitIsCaster) {
                    hitResult = BlockHitResult.miss(hitResult.getLocation(), Direction.getNearest(hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z), BlockPos.containing(hitResult.getLocation()));
                }
                if (hitResult.getType() != HitResult.Type.MISS) {
                    Vec3 hitPos = hitResult.getLocation();
                    serverLevel.sendParticles(particleOptions, hitPos.x, hitPos.y, hitPos.z, 4, 0.1, 0.1, 0.1, 0.02);

                    if (resolver != null) {
                        if (forwardedSpellManaCost <= 0.0 || consumeManaAndAccumulate(serverLevel, forwardedSpellManaCost)) {
                            SpellContext childContext = resolver.spellContext.clone().makeChildContext();
                            SpellResolver childResolver = resolver.getNewResolver(childContext);
                            childResolver.onResolveEffect(serverLevel, hitResult);
                        }
                    }

                    if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity target && !this.isDampened()) {
                        float damage = BASE_DAMAGE;
                        DamageSource damageSource = this.level().damageSources().magic();
                        if (casterUUID != null) {
                            Entity caster = serverLevel.getEntity(casterUUID);
                            if (caster instanceof LivingEntity livingCaster) {
                                damageSource = this.level().damageSources().indirectMagic(this, livingCaster);
                            }
                        }
                        applyBeamDamage(target, damageSource, damage);
                    }
                }
            }
        }
    }

    private void applyBeamDamage(LivingEntity target, DamageSource damageSource, float damage) {
        // Bypass vanilla damage cooldown for beam ticks.
        int previousInvulnerableTime = target.invulnerableTime;
        target.invulnerableTime = 0;
        target.hurt(damageSource, damage);
        target.invulnerableTime = previousInvulnerableTime;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("age") && compound.contains("lifetime")) {
            int oldLifetime = compound.getInt("lifetime");
            int oldAge = compound.getInt("age");
            this.setLifetime(Math.max(0, oldLifetime - oldAge));
        } else {
            this.setLifetime(compound.getInt("lifetime"));
        }
        int maxLifetime = compound.getInt("max_lifetime");
        if (maxLifetime <= 0) {
            maxLifetime = Math.max(this.getLifetime(), DEFAULT_LIFETIME_TICKS);
        }
        this.setMaxLifetime(maxLifetime);
        this.setColor(compound.getFloat("color_r"), compound.getFloat("color_g"), compound.getFloat("color_b"));
        this.setDampened(compound.getBoolean("dampened"));
        if (compound.hasUUID("caster_uuid")) {
            this.casterUUID = compound.getUUID("caster_uuid");
        } else {
            this.casterUUID = null;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("lifetime", this.getLifetime());
        compound.putInt("max_lifetime", this.getMaxLifetime());
        compound.putFloat("color_r", this.getColorR());
        compound.putFloat("color_g", this.getColorG());
        compound.putFloat("color_b", this.getColorB());
        compound.putBoolean("dampened", this.isDampened());
        if (this.casterUUID != null) {
            compound.putUUID("caster_uuid", this.casterUUID);
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public double getManaCostPerBlock() {
        return forwardedSpellManaCost;
    }

    @Override
    public UUID getCasterUUID() {
        return casterUUID;
    }

    @Override
    public double getAccumulatedDrain() {
        return accumulatedDrain;
    }

    @Override
    public void setAccumulatedDrain(double value) {
        this.accumulatedDrain = value;
    }

    @Override
    public int getTicksSinceLastDrainSync() {
        return ticksSinceLastDrainSync;
    }

    @Override
    public void setTicksSinceLastDrainSync(int value) {
        this.ticksSinceLastDrainSync = value;
    }

    @Override
    public Player getCasterPlayer() {
        if (level() instanceof ServerLevel sl && casterUUID != null && sl.getServer() != null) {
            return sl.getServer().getPlayerList().getPlayer(casterUUID);
        }
        return null;
    }

    @Override
    public void setCasterPlayer(Player player) {
    }
}
