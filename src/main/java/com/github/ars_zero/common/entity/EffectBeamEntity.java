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
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.common.block.tile.CreativeSourceJarTile;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.List;
import java.util.UUID;

public class EffectBeamEntity extends Entity implements ILifespanExtendable, IManaDrainable {

    public static final int DEFAULT_LIFETIME_TICKS = 5;
    public static final double RAY_LENGTH = 256.0;

    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAX_LIFETIME = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> COLOR_R = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_G = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_B = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DAMPENED = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IGNORE_ENTITIES = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HAS_TARGET = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> TARGET_X = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Y = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Z = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_TURRET = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TURRET_X = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TURRET_Y = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TURRET_Z = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TURRET_FACING = SynchedEntityData.defineId(EffectBeamEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID casterUUID;
    @Nullable
    protected SpellResolver resolver;

    private double forwardedSpellManaCost = 0.0;
    private double accumulatedDrain = 0.0;
    private int ticksSinceLastDrainSync = 0;
    private float damage = 0.5f;

    public EffectBeamEntity(EntityType<? extends EffectBeamEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.noCulling = true;
        this.setBoundingBox(new AABB(0, 0, 0, 0, 0, 0));
    }

    public EffectBeamEntity(Level level, double x, double y, double z, float yRot, float xRot, int lifetime, float r, float g, float b, @Nullable UUID casterUUID, boolean dampened, boolean ignoreEntities, float damage) {
        this(ModEntities.EFFECT_BEAM.get(), level);
        this.setPos(x, y, z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.setLifetime(lifetime);
        this.setMaxLifetime(lifetime);
        this.setColor(r, g, b);
        this.casterUUID = casterUUID;
        this.setDampened(dampened);
        this.setIgnoreEntities(ignoreEntities);
        this.damage = damage;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public float getPickRadius() {
        return 0.0f;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean isPickable() {
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
    protected AABB makeBoundingBox() {
        Vec3 pos = this.position();
        return new AABB(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 origin = this.position();
        Vec3 end = this.getEffectiveEndPoint(origin);
        double minX = Math.min(origin.x, end.x) - 0.5;
        double minY = Math.min(origin.y, end.y) - 0.5;
        double minZ = Math.min(origin.z, end.z) - 0.5;
        double maxX = Math.max(origin.x, end.x) + 0.5;
        double maxY = Math.max(origin.y, end.y) + 0.5;
        double maxZ = Math.max(origin.z, end.z) + 0.5;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LIFETIME, DEFAULT_LIFETIME_TICKS);
        builder.define(MAX_LIFETIME, DEFAULT_LIFETIME_TICKS);
        builder.define(COLOR_R, 1.0f);
        builder.define(COLOR_G, 1.0f);
        builder.define(COLOR_B, 1.0f);
        builder.define(DAMPENED, false);
        builder.define(IGNORE_ENTITIES, false);
        builder.define(HAS_TARGET, false);
        builder.define(TARGET_X, 0.0f);
        builder.define(TARGET_Y, 0.0f);
        builder.define(TARGET_Z, 0.0f);
        builder.define(IS_TURRET, false);
        builder.define(TURRET_X, 0);
        builder.define(TURRET_Y, 0);
        builder.define(TURRET_Z, 0);
        builder.define(TURRET_FACING, 0);
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

    public boolean isIgnoreEntities() {
        return this.entityData.get(IGNORE_ENTITIES);
    }

    public void setIgnoreEntities(boolean ignoreEntities) {
        this.entityData.set(IGNORE_ENTITIES, ignoreEntities);
    }

    public void setTargetEndpoint(Vec3 target) {
        this.entityData.set(HAS_TARGET, true);
        this.entityData.set(TARGET_X, (float) target.x);
        this.entityData.set(TARGET_Y, (float) target.y);
        this.entityData.set(TARGET_Z, (float) target.z);
    }

    public void setTurretInfo(BlockPos turretPos, Direction facing) {
        this.entityData.set(IS_TURRET, true);
        this.entityData.set(TURRET_X, turretPos.getX());
        this.entityData.set(TURRET_Y, turretPos.getY());
        this.entityData.set(TURRET_Z, turretPos.getZ());
        this.entityData.set(TURRET_FACING, facing.ordinal());
    }

    private void updateRotation(ServerLevel level) {
        Vec3 lookVec;
        
        if (this.entityData.get(IS_TURRET)) {
            Direction facing = Direction.from3DDataValue(this.entityData.get(TURRET_FACING));
            lookVec = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ()).normalize();
        } else {
            if (this.casterUUID == null) {
                return;
            }
            Entity casterEntity = level.getEntity(this.casterUUID);
            if (!(casterEntity instanceof LivingEntity caster)) {
                return;
            }
            lookVec = caster.getLookAngle();
        }
        
        if (lookVec.lengthSqr() >= 1.0E-12) {
            float[] yawPitch = com.github.ars_zero.common.util.MathHelper.vecToYawPitch(lookVec);
            this.setYRot(yawPitch[0]);
            this.setXRot(yawPitch[1]);
        }
    }


    public Vec3 getEffectiveEndPoint(Vec3 origin) {
        if (this.entityData.get(HAS_TARGET)) {
            return new Vec3(this.entityData.get(TARGET_X), this.entityData.get(TARGET_Y), this.entityData.get(TARGET_Z));
        }
        Vec3 forward = this.getForward();
        return origin.add(forward.scale(RAY_LENGTH + 0.5));
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
            updateRotation(serverLevel);
        }

        Vec3 origin = this.position();
        Vec3 forward = this.getForward();
        Vec3 end = origin.add(forward.scale(RAY_LENGTH + 0.5));

        BlockHitResult blockHit = this.level().clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        HitResult hitResult = blockHit;
        if (!this.isIgnoreEntities()) {
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(this, origin, end, this.getBoundingBox().inflate(RAY_LENGTH), e -> e.isPickable() && !e.isSpectator() && e != this, RAY_LENGTH * RAY_LENGTH);
            if (entityHit != null) {
                double entityDist = origin.distanceTo(entityHit.getLocation());
                double blockDist = blockHit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : origin.distanceTo(blockHit.getLocation());
                if (entityDist < blockDist) {
                    hitResult = entityHit;
                }
            }
        }

        Vec3 hitPos;
        if (hitResult.getType() != HitResult.Type.MISS) {
            hitPos = hitResult.getLocation();
        } else {
            hitPos = end;
        }
        
        this.setTargetEndpoint(hitPos);

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
                    serverLevel.sendParticles(particleOptions, hitPos.x, hitPos.y, hitPos.z, 4, 0.1, 0.1, 0.1, 0.02);

                    if (resolver != null) {
                        if (forwardedSpellManaCost <= 0.0 || consumeManaAndAccumulate(serverLevel, forwardedSpellManaCost)) {
                            SpellContext childContext = resolver.spellContext.clone().makeChildContext();
                            SpellResolver childResolver = resolver.getNewResolver(childContext);
                            childResolver.onResolveEffect(serverLevel, hitResult);
                        } else {
                            this.discard();
                            return;
                        }
                    }

                    if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity target && !this.isDampened()) {
                        if (consumeManaAndAccumulate(serverLevel, this.damage)) {
                            DamageSource damageSource = this.level().damageSources().magic();
                            if (casterUUID != null) {
                                Entity caster = serverLevel.getEntity(casterUUID);
                                if (caster instanceof LivingEntity livingCaster) {
                                    damageSource = this.level().damageSources().indirectMagic(this, livingCaster);
                                }
                            }
                            applyBeamDamage(target, damageSource, this.damage);
                        } else {
                            this.discard();
                            return;
                        }
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
        this.setIgnoreEntities(compound.getBoolean("ignore_entities"));
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
        compound.putBoolean("ignore_entities", this.isIgnoreEntities());
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

    @Override
    public boolean consumeManaAndAccumulate(ServerLevel level, double manaCost) {
        if (this.entityData.get(IS_TURRET)) {
            BlockPos turretPos = new BlockPos(this.entityData.get(TURRET_X), this.entityData.get(TURRET_Y), this.entityData.get(TURRET_Z));
            int requested = (int) Math.ceil(manaCost);
            int drained = drainSourceFromTurret(level, turretPos, requested);
            if (drained >= requested) {
                return true;
            }
            return false;
        }
        return IManaDrainable.super.consumeManaAndAccumulate(level, manaCost);
    }

    private int drainSourceFromTurret(ServerLevel serverLevel, BlockPos turretPos, int requested) {
        List<ISpecialSourceProvider> providers = SourceUtil.canTakeSource(turretPos, serverLevel, 10);
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
}
