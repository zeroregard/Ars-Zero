package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import alexthw.ars_elemental.common.glyphs.EffectDischarge;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.github.ars_zero.registry.ModSounds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class LightningVoxelEntity extends BaseVoxelEntity {
    
    private static final int COLOR = 0xFFFFFF;
    private static final double DAMAGE_SPEED_THRESHOLD = 0.35;
    private static final float BASE_DAMAGE = 1.5f;
    private static final float DAMAGE_SCALE = 2.0f;
    private static final float MAX_DAMAGE = 6.0f;
    
    
    public LightningVoxelEntity(EntityType<? extends LightningVoxelEntity> entityType, Level level) {
        super(entityType, level);
    }
    
    public LightningVoxelEntity(Level level, double x, double y, double z, int lifetime) {
        this(ModEntities.LIGHTNING_VOXEL_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.setLifetime(lifetime);
        this.setNoGravityCustom(true);
    }
    
    @Override
    public int getColor() {
        return COLOR;
    }
    
    @Override
    public boolean isEmissive() {
        return true;
    }
    
    @Override
    protected void onBlockCollision(BlockHitResult blockHit) {
        if (handlePhysicalCollision(blockHit)) {
            return;
        }
        
        if (!this.level().isClientSide) {
            Vec3 location = blockHit.getLocation();
            this.level().playSound(null, location.x, location.y, location.z, 
                ModSounds.LIGHTNING_VOXEL_HIT.get(), SoundSource.BLOCKS, 0.4f, 2.0f);
        }
        
        spawnHitParticles(blockHit.getLocation());
        this.discard();
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hit = result.getEntity();
        if (hit instanceof BaseVoxelEntity) {
            super.onHitEntity(result);
            return;
        }
        if (!this.level().isClientSide) {
            Vec3 location = result.getLocation();
            this.level().playSound(null, location.x, location.y, location.z, 
                ModSounds.LIGHTNING_VOXEL_HIT.get(), SoundSource.BLOCKS, 0.4f, 2.0f);
            
            if (hit instanceof LivingEntity living) {
                applyImpactDamage(living);
                castDischargeEffect(living);
                
                // Charge creepers like lightning does
                if (hit instanceof Creeper creeper) {
                    CompoundTag nbt = new CompoundTag();
                    creeper.saveWithoutId(nbt);
                    nbt.putBoolean("powered", true);
                    creeper.load(nbt);
                }
            }
            spawnHitParticles(result.getLocation());
        }
        this.discard();
    }
    
    private void applyImpactDamage(LivingEntity target) {
        double speed = this.getDeltaMovement().length();
        if (speed < DAMAGE_SPEED_THRESHOLD) {
            return;
        }
        float sizeScale = Math.max(1.0f, this.getSize() / BaseVoxelEntity.DEFAULT_BASE_SIZE);
        float damage = BASE_DAMAGE + (float) ((speed - DAMAGE_SPEED_THRESHOLD) * DAMAGE_SCALE);
        damage *= sizeScale;
        damage = Math.min(damage, MAX_DAMAGE);
        LivingEntity sender = this.getStoredCaster();
        net.minecraft.world.damagesource.DamageSource damageSource;
        if (sender != null) {
            damageSource = this.level().damageSources().indirectMagic(this, sender);
            target.setLastHurtByMob(sender);
            if (sender instanceof net.minecraft.world.entity.player.Player) {
                target.setLastHurtByPlayer((net.minecraft.world.entity.player.Player) sender);
            }
        } else {
            damageSource = this.level().damageSources().magic();
        }
        target.hurt(damageSource, damage);
    }
    
    private void castDischargeEffect(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity owner = this.getOwner();
        if (!(owner instanceof LivingEntity caster)) {
            return;
        }
        
        Spell dischargeSpell = new Spell(EffectDischarge.INSTANCE);
        SpellContext context = new SpellContext(serverLevel, dischargeSpell, caster, LivingCaster.from(caster));
        context.setCasterTool(ItemStack.EMPTY);
        
        SpellResolver resolver = new SpellResolver(context).withSilent(true);
        EntityHitResult hitResult = new EntityHitResult(target);
        resolver.onResolveEffect(serverLevel, hitResult);
    }
    
    @Override
    protected ParticleOptions getAmbientParticle() {
        return ParticleTypes.ELECTRIC_SPARK;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide) {
            if (this.age % 5 == 0) {
                spawnAmbientElectricParticles();
            }
            if (this.age % 20 == 0 && this.random.nextInt(2) == 0) {
                spawnStaticBurst();
            }
        } else {
            if (this.age % 8 == 0 && this.random.nextInt(2) == 0) {
                spawnClientStaticEffect();
            }
        }
    }
    
    private void spawnAmbientElectricParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        float size = this.getSize();
        int count = 2 + this.random.nextInt(3);
        
        for (int i = 0; i < count; i++) {
            double theta = this.random.nextDouble() * 2 * Math.PI;
            double phi = this.random.nextDouble() * Math.PI;
            double radius = size * (0.3 + this.random.nextDouble() * 0.4);
            
            double x = this.getX() + radius * Math.sin(phi) * Math.cos(theta);
            double y = this.getY() + radius * Math.sin(phi) * Math.sin(theta);
            double z = this.getZ() + radius * Math.cos(phi);
            
            double velX = (this.random.nextDouble() - 0.5) * 0.1;
            double velY = (this.random.nextDouble() - 0.5) * 0.1;
            double velZ = (this.random.nextDouble() - 0.5) * 0.1;
            
            serverLevel.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                x, y, z,
                1,
                velX, velY, velZ,
                0.03
            );
        }
    }
    
    private void spawnStaticBurst() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        float size = this.getSize();
        int burstCount = 5 + this.random.nextInt(6);
        
        for (int i = 0; i < burstCount; i++) {
            double angle = this.random.nextDouble() * 2 * Math.PI;
            double distance = size * (0.4 + this.random.nextDouble() * 0.75);
            double height = (this.random.nextDouble() - 0.5) * size * 0.8;
            
            double x = this.getX() + Math.cos(angle) * distance;
            double y = this.getY() + height;
            double z = this.getZ() + Math.sin(angle) * distance;
            
            double velX = (this.random.nextDouble() - 0.5) * 0.15;
            double velY = (this.random.nextDouble() - 0.5) * 0.15;
            double velZ = (this.random.nextDouble() - 0.5) * 0.15;
            
            serverLevel.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                x, y, z,
                1,
                velX, velY, velZ,
                0.05
            );
        }
    }
    
    private void spawnClientStaticEffect() {
        if (!(this.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)) {
            return;
        }
        
        float size = this.getSize();
        int sparkCount = 4 + this.random.nextInt(5);
        
        for (int i = 0; i < sparkCount; i++) {
            double angle = this.random.nextDouble() * 2 * Math.PI;
            double distance = size * (0.5 + this.random.nextDouble() * 0.5);
            double height = (this.random.nextDouble() - 0.5) * size;
            
            double x = this.getX() + Math.cos(angle) * distance;
            double y = this.getY() + height;
            double z = this.getZ() + Math.sin(angle) * distance;
            
            double velX = (this.random.nextDouble() - 0.5) * 0.2;
            double velY = (this.random.nextDouble() - 0.5) * 0.2;
            double velZ = (this.random.nextDouble() - 0.5) * 0.2;
            
            clientLevel.addParticle(
                ParticleTypes.ELECTRIC_SPARK,
                x, y, z,
                velX, velY, velZ
            );
        }
    }
    
    @Override
    protected void spawnHitParticles(Vec3 location) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ParticleOptions option = ParticleTypes.ELECTRIC_SPARK;
        int particleCount = Math.max(12, (int) (this.getSize() / BaseVoxelEntity.DEFAULT_BASE_SIZE) * 8);
        
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 0.6;
            double offsetY = (this.random.nextDouble() - 0.5) * 0.4;
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.6;
            
            double velX = (this.random.nextDouble() - 0.5) * 0.2;
            double velY = (this.random.nextDouble() - 0.5) * 0.2;
            double velZ = (this.random.nextDouble() - 0.5) * 0.2;
            
            serverLevel.sendParticles(
                option,
                location.x + offsetX,
                location.y + offsetY,
                location.z + offsetZ,
                1,
                velX, velY, velZ,
                0.02
            );
        }
    }
}
