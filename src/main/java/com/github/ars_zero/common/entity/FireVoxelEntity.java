package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.setup.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.FluidTags;

public class FireVoxelEntity extends BaseVoxelEntity {
    
    private static final int COLOR = 0xFF6A00;
    private static final float MIN_SIZE_THRESHOLD = 0.0625f;
    private static final double DAMAGE_SPEED_THRESHOLD = 0.35;
    private static final float BASE_DAMAGE = 1.5f;
    private static final float DAMAGE_SCALE = 2.0f;
    private static final float MAX_DAMAGE = 6.0f;
    
    private float casterFirePower = 0.0f;
    
    public FireVoxelEntity(EntityType<? extends FireVoxelEntity> entityType, Level level) {
        super(entityType, level);
    }
    
    public FireVoxelEntity(Level level, double x, double y, double z, int lifetime) {
        this(ModEntities.FIRE_VOXEL_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.setLifetime(lifetime);
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
        BlockPos hitPos = blockHit.getBlockPos();
        BlockState hitState = this.level().getBlockState(hitPos);
        
        if (hitState.is(Blocks.CAMPFIRE) && !hitState.getValue(BlockStateProperties.LIT)) {
            this.level().setBlock(hitPos, hitState.setValue(BlockStateProperties.LIT, true), 3);
            return;
        }
        if (hitState.is(Blocks.TNT)) {
            if (!this.level().isClientSide) {
                this.level().removeBlock(hitPos, false);
                PrimedTnt primedTnt = new PrimedTnt(this.level(), hitPos.getX() + 0.5D, hitPos.getY(), hitPos.getZ() + 0.5D, null);
                this.level().addFreshEntity(primedTnt);
                this.level().playSound(
                    null,
                    primedTnt.getX(),
                    primedTnt.getY(),
                    primedTnt.getZ(),
                    SoundEvents.TNT_PRIMED,
                    SoundSource.BLOCKS,
                    1.0f,
                    1.0f
                );
            }
            return;
        }
        if (hitState.is(Blocks.ICE)) {
            if (!this.level().isClientSide) {
                this.level().setBlock(hitPos, Blocks.WATER.defaultBlockState(), 3);
            }
            return;
        }
        if (hitState.is(Blocks.SNOW) || hitState.is(Blocks.SNOW_BLOCK)) {
            if (!this.level().isClientSide) {
                this.level().removeBlock(hitPos, false);
            }
            return;
        }
        
        if (hitState.getBlock() == Blocks.WATER) {
            if (canEvaporateWater(hitState)) {
                this.level().setBlock(hitPos, Blocks.AIR.defaultBlockState(), 3);
                spawnEvaporationParticles(Vec3.atCenterOf(hitPos));
            }
            return;
        }
        
        BlockPos placePos = hitPos.relative(blockHit.getDirection());
        BlockState placeState = this.level().getBlockState(placePos);
        
        if (placeState.getBlock() == Blocks.WATER) {
            if (canEvaporateWater(placeState)) {
                this.level().setBlock(placePos, Blocks.AIR.defaultBlockState(), 3);
                spawnEvaporationParticles(Vec3.atCenterOf(placePos));
            }
        } else if (placeState.isAir()) {
            if (tryActivatePortal(placePos)) {
                return;
            }
            this.level().setBlock(placePos, Blocks.FIRE.defaultBlockState(), 3);
        } else {
            // General block impact sound
            if (!this.level().isClientSide) {
                Vec3 location = blockHit.getLocation();
                this.level().playSound(null, location.x, location.y, location.z, 
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.0f + this.random.nextFloat() * 0.3f);
            }
        }
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    protected void onHitEntity(net.minecraft.world.phys.EntityHitResult result) {
        if (result.getEntity() instanceof BaseVoxelEntity) {
            super.onHitEntity(result);
            return;
        }
        
        if (!this.level().isClientSide) {
            Vec3 location = result.getLocation();
            this.level().playSound(null, location.x, location.y, location.z, 
                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.0f + this.random.nextFloat() * 0.3f);
            
            Entity hitEntity = result.getEntity();
            
            if (hitEntity instanceof LivingEntity living) {
                applyImpactDamage(living);
            }
            
            Vec3 hitLocation = result.getLocation();
            
            BlockPos centerPos = new BlockPos(
                (int) Math.round(hitLocation.x),
                (int) Math.round(hitLocation.y),
                (int) Math.round(hitLocation.z)
            );
            
            for (BlockPos pos : BlockPos.betweenClosed(centerPos.offset(-1, -1, -1), centerPos.offset(1, 1, 1))) {
                BlockState state = this.level().getBlockState(pos);
                if (state.getBlock() == Blocks.WATER && canEvaporateWater(state)) {
                    this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    spawnEvaporationParticles(Vec3.atCenterOf(pos));
                    break;
                } else if (state.isAir()) {
                    this.level().setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                    break;
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
        Entity owner = this.getOwner();
        LivingEntity sender = owner instanceof LivingEntity ? (LivingEntity) owner : null;
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
        int fireDuration = Math.max(20, (int) ((this.getSize() / BaseVoxelEntity.DEFAULT_BASE_SIZE) * 20));
        target.setRemainingFireTicks(target.getRemainingFireTicks() + fireDuration);
        Vec3 impulse = this.getDeltaMovement().scale(0.35);
        target.push(impulse.x, Math.max(0.1, impulse.y + 0.15), impulse.z);
        target.hurtMarked = true;
    }
    
    private boolean canEvaporateWater(BlockState waterState) {
        if (waterState.getBlock() != Blocks.WATER) {
            return false;
        }
        
        int waterLevel = waterState.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL);
        
        if (waterLevel == 0) {
            return this.getSize() >= 1.0f;
        }
        
        return true;
    }
    
    private void spawnEvaporationParticles(Vec3 location) {
        if (!this.level().isClientSide) {
            for (int i = 0; i < 20; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
                double offsetY = this.random.nextDouble() * 0.5;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;
                ((ServerLevel) this.level()).sendParticles(
                    ParticleTypes.CLOUD,
                    location.x + offsetX,
                    location.y + offsetY,
                    location.z + offsetZ,
                    1,
                    0.0, 0.1, 0.0,
                    0.02
                );
            }
            for (int i = 0; i < 10; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.3;
                double offsetY = (this.random.nextDouble() - 0.5) * 0.3;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.3;
                ((ServerLevel) this.level()).sendParticles(
                    ParticleTypes.SMOKE,
                    location.x + offsetX,
                    location.y + offsetY,
                    location.z + offsetZ,
                    1,
                    0.0, 0.05, 0.0,
                    0.01
                );
            }
        }
    }
    
    private int calculateParticleCount() {
        float size = this.getSize();
        float ratio = size / 1.0f;
        
        int baseCount = (int) (ratio * 32);
        return Math.min(baseCount, 32);
    }

    private boolean tryActivatePortal(BlockPos placePos) {
        if (this.level().isClientSide) {
            return false;
        }
        if (!this.level().getBlockState(placePos).isAir()) {
            return false;
        }
        if (attemptPortalWithAxis(placePos, Direction.Axis.X)) {
            return true;
        }
        return attemptPortalWithAxis(placePos, Direction.Axis.Z);
    }

    private boolean attemptPortalWithAxis(BlockPos origin, Direction.Axis axis) {
        if (!isPortalFrameIntact(origin, axis)) {
            return false;
        }
        BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, axis);
        Direction horizontalStep = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        for (int x = 0; x < 2; x++) {
            BlockPos columnBase = origin.relative(horizontalStep, x);
            for (int y = 0; y < 3; y++) {
                this.level().setBlock(columnBase.above(y), portalState, 18);
            }
        }
        return true;
    }

    private boolean isPortalFrameIntact(BlockPos origin, Direction.Axis axis) {
        Direction horizontalStep = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction opposite = horizontalStep.getOpposite();
        BlockPos belowOrigin = origin.below();
        if (!isObsidian(belowOrigin)) {
            return false;
        }
        for (int column = 0; column < 2; column++) {
            BlockPos columnBase = origin.relative(horizontalStep, column);
            if (!isObsidian(columnBase.below())) {
                return false;
            }
            if (!isObsidian(columnBase.above(3))) {
                return false;
            }
            for (int height = 0; height < 3; height++) {
                BlockState interiorState = this.level().getBlockState(columnBase.above(height));
                if (!interiorState.isAir() && !interiorState.is(Blocks.FIRE)) {
                    return false;
                }
            }
        }
        BlockPos leftColumn = origin.relative(opposite);
        BlockPos rightColumn = origin.relative(horizontalStep, 2);
        for (int height = 0; height < 3; height++) {
            if (!isObsidian(leftColumn.above(height))) {
                return false;
            }
            if (!isObsidian(rightColumn.above(height))) {
                return false;
            }
        }
        return true;
    }

    private boolean isObsidian(BlockPos pos) {
        return this.level().getBlockState(pos).is(Blocks.OBSIDIAN);
    }
    
    private void handleRainDampening() {
        if (!isInEffectiveRain()) {
            return;
        }
        float shrinkPercent = getRainDampeningPercent();
        if (shrinkPercent <= 0.0f) {
            return;
        }
        float newSize = this.getSize() * (1.0f - shrinkPercent);
        if (newSize < MIN_SIZE_THRESHOLD) {
            spawnRainSteamParticles();
            playRainSteamSound();
            this.discard();
            return;
        }
        this.setSize(newSize);
        this.refreshDimensions();
        spawnRainSteamParticles();
        playRainSteamSound();
    }
    
    private void spawnRainSteamParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 pos = this.position();
        int particleCount = 3 + this.random.nextInt(3);
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * this.getSize() * 0.6;
            double offsetY = this.random.nextDouble() * this.getSize() * 0.4;
            double offsetZ = (this.random.nextDouble() - 0.5) * this.getSize() * 0.6;
            serverLevel.sendParticles(
                ParticleTypes.CLOUD,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                1,
                0.0, 0.05, 0.0,
                0.01
            );
        }
    }
    
    private void playRainSteamSound() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 pos = this.position();
        serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 1.5f + this.random.nextFloat() * 0.3f);
    }
    
    private boolean isInEffectiveRain() {
        if (this.level().isClientSide) {
            return false;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!serverLevel.isRaining()) {
            return false;
        }
        BlockPos pos = this.blockPosition();
        return serverLevel.canSeeSky(pos);
    }
    
    private float getRainDampeningPercent() {
        if (this.casterFirePower >= 2.0f) {
            return 0.0f;
        }
        if (this.casterFirePower >= 1.0f) {
            return 0.025f;
        }
        return 0.05f;
    }
    
    private void handleUnderwaterDampening() {
        evaporateAdjacentWater();
        float shrinkPercent = getUnderwaterDampeningPercent();
        if (shrinkPercent <= 0.0f) {
            return;
        }
        float newSize = this.getSize() * (1.0f - shrinkPercent);
        if (newSize < MIN_SIZE_THRESHOLD) {
            this.discard();
            return;
        }
        this.setSize(newSize);
        this.refreshDimensions();
    }
    
    private void evaporateAdjacentWater() {
        if (this.level().isClientSide) {
            return;
        }
        boolean removedAny = false;
        BlockPos center = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            BlockState state = this.level().getBlockState(pos);
            if (state.getFluidState().is(FluidTags.WATER)) {
                this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                spawnEvaporationParticles(Vec3.atCenterOf(pos));
                removedAny = true;
            }
        }
        if (removedAny) {
            playEvaporationSound(Vec3.atCenterOf(center));
        }
    }
    
    private void playEvaporationSound(Vec3 location) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, location.x, location.y, location.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1.2f + this.random.nextFloat() * 0.2f);
    }
    
    private boolean isSubmergedInWater() {
        return this.isInWater();
    }
    
    private float getUnderwaterDampeningPercent() {
        if (this.casterFirePower >= 2.0f) {
            return 0.0f;
        }
        if (this.casterFirePower >= 1.0f) {
            return 0.25f;
        }
        return 0.5f;
    }
    
    @Override
    protected net.minecraft.core.particles.ParticleOptions getAmbientParticle() {
        return this.random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SMOKE;
    }
    
    @Override
    public void tick() {
        if (!this.level().isClientSide && this.isAlive()) {
            if (isSubmergedInWater()) {
                handleUnderwaterDampening();
                if (!this.isAlive()) {
                    return;
                }
            } else if (this.tickCount % 20 == 0) {
                handleRainDampening();
                if (!this.isAlive()) {
                    return;
                }
            }
        }
        
        super.tick();
        
        if (!this.level().isClientSide) {
            Vec3 motion = this.getDeltaMovement();
            double speed = motion.length();
            
            if (speed > 0.05) {
                int trailCount = (int) (speed * 10);
                for (int i = 0; i < Math.min(trailCount, 5); i++) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 0.2;
                    double offsetY = (this.random.nextDouble() - 0.5) * 0.2;
                    double offsetZ = (this.random.nextDouble() - 0.5) * 0.2;
                    
                    ((ServerLevel) this.level()).sendParticles(
                        ParticleTypes.FLAME,
                        this.getX() + offsetX,
                        this.getY() + offsetY,
                        this.getZ() + offsetZ,
                        1,
                        0.0, 0.0, 0.0,
                        0.01
                    );
                    
                    if (this.random.nextFloat() < 0.3f) {
                        ((ServerLevel) this.level()).sendParticles(
                            ParticleTypes.SMOKE,
                            this.getX() + offsetX,
                            this.getY() + offsetY,
                            this.getZ() + offsetZ,
                            1,
                            0.0, 0.0, 0.0,
                            0.005
                        );
                    }
                }
            }
            
            if (this.age % 5 == 0) {
                for (int i = 0; i < 2; i++) {
                    double offsetX = (this.random.nextDouble() - 0.5) * this.getSize() * 0.5;
                    double offsetY = (this.random.nextDouble() - 0.5) * this.getSize() * 0.5;
                    double offsetZ = (this.random.nextDouble() - 0.5) * this.getSize() * 0.5;
                    
                    ((ServerLevel) this.level()).sendParticles(
                        ParticleTypes.SMOKE,
                        this.getX() + offsetX,
                        this.getY() + offsetY,
                        this.getZ() + offsetZ,
                        1,
                        0.0, 0.02, 0.0,
                        0.01
                    );
                }
            }
        }
    }
    
    @Override
    protected void spawnHitParticles(Vec3 location) {
        if (!this.level().isClientSide) {
            int particleCount = calculateParticleCount();
            
            for (int i = 0; i < particleCount; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.3;
                double offsetY = (this.random.nextDouble() - 0.5) * 0.3;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.3;
                ((ServerLevel) this.level()).sendParticles(
                    ParticleTypes.FLAME,
                    location.x + offsetX,
                    location.y + offsetY,
                    location.z + offsetZ,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
                );
            }
            for (int i = 0; i < particleCount / 2; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.2;
                double offsetY = (this.random.nextDouble() - 0.5) * 0.2;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.2;
                ((ServerLevel) this.level()).sendParticles(
                    ParticleTypes.SMOKE,
                    location.x + offsetX,
                    location.y + offsetY,
                    location.z + offsetZ,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
                );
            }
        }
    }
    
    public void setCasterFirePower(float firePower) {
        this.casterFirePower = Math.max(0.0f, firePower);
    }
    
    public float getCasterFirePower() {
        return this.casterFirePower;
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("FirePower")) {
            this.casterFirePower = compound.getFloat("FirePower");
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.casterFirePower != 0.0f) {
            compound.putFloat("FirePower", this.casterFirePower);
        }
    }
}

