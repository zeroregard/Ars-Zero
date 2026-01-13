package com.github.ars_zero.common.entity;

import com.github.ars_zero.common.block.BlightCauldronBlock;
import com.github.ars_zero.common.block.BlightLiquidBlock;
import com.github.ars_zero.registry.ModBlocks;
import com.github.ars_zero.registry.ModEntities;
import com.github.ars_zero.registry.ModFluids;
import com.github.ars_zero.registry.ModParticles;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class BlightVoxelEntity extends BaseVoxelEntity {
    private static final int COLOR = 0xFFFFFF;
    private static final ResourceKey<net.minecraft.world.effect.MobEffect> WITHER_EFFECT_KEY =
        ResourceKey.create(Registries.MOB_EFFECT, ResourceLocation.parse("minecraft:wither"));

    public BlightVoxelEntity(EntityType<? extends BlightVoxelEntity> entityType, Level level) {
        super(entityType, level);
    }

    public BlightVoxelEntity(Level level, double x, double y, double z, int lifetime) {
        this(ModEntities.BLIGHT_VOXEL_ENTITY.get(), level);
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
        return null;
    }
    
    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity instanceof BaseVoxelEntity) {
            return super.canHitEntity(entity);
        }
        if (entity instanceof ItemEntity) {
            return true;
        }
        return super.canHitEntity(entity);
    }
    
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.isAlive()) {
            if (handleLavaContact()) {
                return;
            }
            BlockPos currentPos = this.blockPosition();
            BlockState currentState = this.level().getBlockState(currentPos);
            if (currentState.getBlock() == Blocks.WATER || currentState.getFluidState().is(FluidTags.WATER)) {
                spawnVaporizationEffects(this.position());
                this.discard();
                return;
            }
            for (Direction dir : Direction.values()) {
                BlockPos adjacentPos = currentPos.relative(dir);
                BlockState adjacentState = this.level().getBlockState(adjacentPos);
                if (adjacentState.getBlock() == Blocks.WATER || adjacentState.getFluidState().is(FluidTags.WATER)) {
                    spawnVaporizationEffects(this.position());
                    this.discard();
                    return;
                }
            }
            if (this.level().isRainingAt(currentPos)) {
                spawnVaporizationEffects(this.position());
                this.discard();
                return;
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof BaseVoxelEntity) {
            super.onHitEntity(result);
            return;
        }
        if (!this.level().isClientSide) {
            Entity hitEntity = result.getEntity();
            
            if (hitEntity instanceof ItemEntity itemEntity) {
                convertBucketToBlight(itemEntity);
            } else if (hitEntity instanceof Sheep sheep && !sheep.isSheared() && sheep.readyForShearing()) {
                shearSheep(sheep, result.getLocation());
                applyDamage(sheep);
                applyEffect(sheep, true);
                spawnVaporizationEffects(result.getLocation());
            } else if (hitEntity instanceof LivingEntity living) {
                applyDamage(living);
                applyEffect(living, true);
                spawnVaporizationEffects(result.getLocation());
            }
        }
        spawnHitParticles(result.getLocation());
        this.discard();
    }
    
    private void convertBucketToBlight(ItemEntity itemEntity) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        ItemStack stack = itemEntity.getItem();
        if (stack.is(Items.BUCKET)) {
            ItemStack blightBucket = new ItemStack(ModFluids.BLIGHT_FLUID_BUCKET.get());
            itemEntity.setItem(blightBucket);
            serverLevel.playSound(null, itemEntity, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            
            for (int i = 0; i < 6; i++) {
                double ox = (serverLevel.random.nextDouble() - 0.5D) * 0.2;
                double oy = (serverLevel.random.nextDouble() - 0.5D) * 0.2;
                double oz = (serverLevel.random.nextDouble() - 0.5D) * 0.2;
                serverLevel.sendParticles(ModParticles.BLIGHT_SPLASH.get(), 
                    itemEntity.getX() + ox, itemEntity.getY() + oy, itemEntity.getZ() + oz, 
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }
    
    private void shearSheep(Sheep sheep, Vec3 location) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        sheep.setSheared(true);
        
        for (int i = 0; i < 12; i++) {
            double ox = (serverLevel.random.nextDouble() - 0.5D) * 0.4;
            double oy = (serverLevel.random.nextDouble() - 0.5D) * 0.4;
            double oz = (serverLevel.random.nextDouble() - 0.5D) * 0.4;
            serverLevel.sendParticles(ParticleTypes.SMOKE, location.x + ox, location.y + oy, location.z + oz, 1, 0.0D, 0.05D, 0.0D, 0.02D);
        }
        
        for (int i = 0; i < 6; i++) {
            double ox = (serverLevel.random.nextDouble() - 0.5D) * 0.3;
            double oy = (serverLevel.random.nextDouble() - 0.5D) * 0.3;
            double oz = (serverLevel.random.nextDouble() - 0.5D) * 0.3;
            serverLevel.sendParticles(ModParticles.BLIGHT_SPLASH.get(), location.x + ox, location.y + oy, location.z + oz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
    

    @Override
    protected void onBlockCollision(BlockHitResult blockHit) {
        if (!this.level().isClientSide) {
            BlockPos hitPos = blockHit.getBlockPos();
            BlockState hitState = this.level().getBlockState(hitPos);
            
            if (hitState.getFluidState().is(FluidTags.LAVA)) {
                spawnVaporizationEffects(blockHit.getLocation());
                this.discard();
                return;
            }
            
            if (hitState.getBlock() == Blocks.WATER || hitState.getFluidState().is(FluidTags.WATER)) {
                spawnVaporizationEffects(blockHit.getLocation());
                this.discard();
                return;
            }
            
            Direction hitDirection = blockHit.getDirection();
            boolean isTopHit = hitDirection == Direction.UP;
            boolean shouldPlaceLiquid = isTopHit;
            
            if (!(this.level() instanceof ServerLevel serverLevel)) {
                spawnHitParticles(blockHit.getLocation());
                return;
            }
            
            if (hitState.is(Blocks.CAULDRON) && isTopHit) {
                serverLevel.setBlock(
                    hitPos,
                    ModBlocks.BLIGHT_CAULDRON.get().defaultBlockState().setValue(BlightCauldronBlock.LEVEL, 1),
                    3
                );
                spawnHitParticles(blockHit.getLocation());
                return;
            }
            if (hitState.getBlock() == ModBlocks.BLIGHT_CAULDRON.get() && isTopHit) {
                int currentLevel = hitState.getValue(BlightCauldronBlock.LEVEL);
                if (currentLevel < BlightCauldronBlock.MAX_FILL_LEVEL) {
                    serverLevel.setBlock(
                        hitPos,
                        hitState.setValue(BlightCauldronBlock.LEVEL, currentLevel + 1),
                        3
                    );
                    spawnHitParticles(blockHit.getLocation());
                    return;
                }
            }
            
            if (hitState.is(BlockTags.LOGS)) {
                spawnVaporizationEffects(blockHit.getLocation());
                if (shouldPlaceLiquid) {
                    BlockPos placePos = hitPos.above();
                    placeBlightFluidWithUnits(placePos);
                }
            } else if (hitState.is(BlockTags.LEAVES)) {
                serverLevel.destroyBlock(hitPos, false);
                spawnVaporizationEffects(blockHit.getLocation());
            } else if (isVegetation(hitState) && isTopHit) {
                destroyVegetation(hitPos, hitState, blockHit);
                if (shouldPlaceLiquid) {
                    BlockPos placePos = hitPos.above();
                    placeBlightFluidWithUnits(placePos);
                }
            } else if (shouldConvertToDirt(hitState) && isTopHit) {
                BlockPos abovePos = hitPos.above();
                BlockState aboveState = this.level().getBlockState(abovePos);
                if (isVegetation(aboveState)) {
                    destroyVegetation(abovePos, aboveState, blockHit);
                }
                serverLevel.setBlock(hitPos, Blocks.DIRT.defaultBlockState(), 3);
                spawnVaporizationEffects(blockHit.getLocation());
                if (shouldPlaceLiquid) {
                    BlockPos placePos = hitPos.above();
                    placeBlightFluidWithUnits(placePos);
                }
            } else if (shouldConvertMossy(hitState)) {
                convertMossyBlock(serverLevel, hitPos, hitState);
                spawnVaporizationEffects(blockHit.getLocation());
                if (shouldPlaceLiquid) {
                    BlockPos placePos = hitPos.above();
                    placeBlightFluidWithUnits(placePos);
                }
            }
            spawnHitParticles(blockHit.getLocation());
        }
    }
    
    private static final TagKey<net.minecraft.world.level.block.Block> REPLACEABLE_PLANTS = 
        TagKey.create(Registries.BLOCK, ResourceLocation.parse("minecraft:replaceable_plants"));
    
    private boolean isVegetation(BlockState state) {
        if (state.is(BlockTags.FLOWERS) || state.is(REPLACEABLE_PLANTS)) {
            return true;
        }
        net.minecraft.world.level.block.Block block = state.getBlock();
        return block == Blocks.SHORT_GRASS || 
               block == Blocks.TALL_GRASS || 
               block == Blocks.FERN || 
               block == Blocks.LARGE_FERN ||
               block == Blocks.DANDELION ||
               block == Blocks.POPPY ||
               block == Blocks.SUGAR_CANE;
    }
    
    private boolean shouldConvertToDirt(BlockState state) {
        net.minecraft.world.level.block.Block block = state.getBlock();
        return block == Blocks.GRASS_BLOCK ||
               block == Blocks.MYCELIUM ||
               block == Blocks.PODZOL ||
               block == Blocks.COARSE_DIRT ||
               block == Blocks.FARMLAND ||
               block == Blocks.MOSS_BLOCK;
    }
    
    private boolean shouldConvertMossy(BlockState state) {
        net.minecraft.world.level.block.Block block = state.getBlock();
        return block == Blocks.MOSSY_COBBLESTONE ||
               block == Blocks.MOSSY_STONE_BRICKS ||
               block == Blocks.MOSSY_COBBLESTONE_STAIRS ||
               block == Blocks.MOSSY_COBBLESTONE_SLAB ||
               block == Blocks.MOSSY_COBBLESTONE_WALL ||
               block == Blocks.MOSSY_STONE_BRICK_STAIRS ||
               block == Blocks.MOSSY_STONE_BRICK_SLAB ||
               block == Blocks.MOSSY_STONE_BRICK_WALL;
    }
    
    private void convertMossyBlock(ServerLevel level, BlockPos pos, BlockState oldState) {
        net.minecraft.world.level.block.Block oldBlock = oldState.getBlock();
        net.minecraft.world.level.block.Block newBlock = null;
        
        if (oldBlock == Blocks.MOSSY_COBBLESTONE) {
            newBlock = Blocks.COBBLESTONE;
        } else if (oldBlock == Blocks.MOSSY_STONE_BRICKS) {
            newBlock = Blocks.STONE_BRICKS;
        } else if (oldBlock == Blocks.MOSSY_COBBLESTONE_STAIRS) {
            newBlock = Blocks.COBBLESTONE_STAIRS;
        } else if (oldBlock == Blocks.MOSSY_COBBLESTONE_SLAB) {
            newBlock = Blocks.COBBLESTONE_SLAB;
        } else if (oldBlock == Blocks.MOSSY_COBBLESTONE_WALL) {
            newBlock = Blocks.COBBLESTONE_WALL;
        } else if (oldBlock == Blocks.MOSSY_STONE_BRICK_STAIRS) {
            newBlock = Blocks.STONE_BRICK_STAIRS;
        } else if (oldBlock == Blocks.MOSSY_STONE_BRICK_SLAB) {
            newBlock = Blocks.STONE_BRICK_SLAB;
        } else if (oldBlock == Blocks.MOSSY_STONE_BRICK_WALL) {
            newBlock = Blocks.STONE_BRICK_WALL;
        }
        
        if (newBlock != null) {
            BlockState newState = BlightLiquidBlock.copyProperties(oldState, newBlock.defaultBlockState());
            level.setBlock(pos, newState, 3);
        }
    }
    
    private void destroyVegetation(BlockPos pos, BlockState vegetationState, BlockHitResult blockHit) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        net.minecraft.world.level.block.Block block = vegetationState.getBlock();
        if (block instanceof DoublePlantBlock) {
            if (vegetationState.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER) {
                BlockPos upperPos = pos.above();
                BlockState upperState = serverLevel.getBlockState(upperPos);
                if (upperState.getBlock() == block && upperState.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
                    serverLevel.setBlock(upperPos, Blocks.AIR.defaultBlockState(), 3);
                }
            } else {
                BlockPos lowerPos = pos.below();
                BlockState lowerState = serverLevel.getBlockState(lowerPos);
                if (lowerState.getBlock() == block && lowerState.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER) {
                    serverLevel.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            serverLevel.destroyBlock(pos, false);
        } else if (block == Blocks.SUGAR_CANE) {
            BlockPos currentPos = pos;
            while (serverLevel.getBlockState(currentPos).getBlock() == Blocks.SUGAR_CANE) {
                serverLevel.destroyBlock(currentPos, false);
                currentPos = currentPos.above();
            }
        } else {
            serverLevel.destroyBlock(pos, false);
        }
        
        serverLevel.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.0f);
    }

    @Override
    protected void spawnHitParticles(Vec3 location) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < 16; i++) {
            double ox = (this.random.nextDouble() - 0.5D) * 0.45;
            double oy = (this.random.nextDouble() - 0.5D) * 0.45;
            double oz = (this.random.nextDouble() - 0.5D) * 0.45;
            serverLevel.sendParticles(ModParticles.BLIGHT_SPLASH.get(), location.x + ox, location.y + oy, location.z + oz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
    
    private boolean handleLavaContact() {
        BlockPos currentPos = this.blockPosition();
        BlockState state = this.level().getBlockState(currentPos);
        if (state.getFluidState().is(FluidTags.LAVA)) {
            spawnVaporizationEffects(this.position());
            this.discard();
            return true;
        }
        for (Direction dir : Direction.values()) {
            BlockPos adjacentPos = currentPos.relative(dir);
            BlockState adjacentState = this.level().getBlockState(adjacentPos);
            if (adjacentState.getFluidState().is(FluidTags.LAVA)) {
                spawnVaporizationEffects(this.position());
                this.discard();
                return true;
            }
        }
        return false;
    }
    
    private void spawnVaporizationEffects(Vec3 location) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < 12; i++) {
            double ox = (this.random.nextDouble() - 0.5D) * 0.4;
            double oy = (this.random.nextDouble() - 0.5D) * 0.4;
            double oz = (this.random.nextDouble() - 0.5D) * 0.4;
            serverLevel.sendParticles(ParticleTypes.SMOKE, location.x + ox, location.y + oy, location.z + oz, 1, 0.0D, 0.05D, 0.0D, 0.02D);
        }
        for (int i = 0; i < 8; i++) {
            double ox = (this.random.nextDouble() - 0.5D) * 0.3;
            double oy = (this.random.nextDouble() - 0.5D) * 0.3;
            double oz = (this.random.nextDouble() - 0.5D) * 0.3;
            serverLevel.sendParticles(ModParticles.BLIGHT_SPLASH.get(), location.x + ox, location.y + oy, location.z + oz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        serverLevel.playSound(null, location.x, location.y, location.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.0f);
    }

    private void applyDamage(LivingEntity target) {
        float sizeScale = Math.max(1.0f, this.getSize() / BaseVoxelEntity.DEFAULT_BASE_SIZE);
        float damage = 2.0f * sizeScale;
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
        target.hurtMarked = true;
    }
    
    private void applyEffect(LivingEntity living, boolean burst) {
        Holder<net.minecraft.world.effect.MobEffect> effect = resolveWitherEffect();
        int duration = getEffectDuration(burst);
        int amplifier = getEffectAmplifier(burst);
        living.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
    }

    private Holder<net.minecraft.world.effect.MobEffect> resolveWitherEffect() {
        return BuiltInRegistries.MOB_EFFECT.getHolder(WITHER_EFFECT_KEY)
            .orElseThrow(() -> new IllegalStateException("Wither effect not found"));
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

    private int levelToUnits(int level) {
        int clamped = Math.max(0, Math.min(7, level));
        return 7 - clamped;
    }
    
    private int unitsToLevel(int units) {
        int clamped = Math.max(0, Math.min(7, units));
        return 7 - clamped;
    }
    
    private int additionalUnitsFromSize() {
        float size = this.getSize();
        float mediumThreshold = BaseVoxelEntity.DEFAULT_BASE_SIZE * 2.0f;
        float fullThreshold = BaseVoxelEntity.DEFAULT_BASE_SIZE * 3.0f;
        if (size >= fullThreshold) {
            return 7;
        }
        if (size >= mediumThreshold) {
            return 4;
        }
        return 1;
    }
    
    private boolean placeBlightFluidWithUnits(BlockPos pos) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockState state = serverLevel.getBlockState(pos);
        if (!state.isAir() && state.getBlock() != ModFluids.BLIGHT_FLUID_BLOCK.get()) {
            return false;
        }
        int additionalUnits = additionalUnitsFromSize();
        if (additionalUnits <= 0) {
            return false;
        }
        int existingUnits = 0;
        if (state.getBlock() == ModFluids.BLIGHT_FLUID_BLOCK.get()) {
            existingUnits = levelToUnits(state.getValue(LiquidBlock.LEVEL));
        }
        int combinedUnits = Math.min(7, existingUnits + additionalUnits);
        if (combinedUnits == existingUnits) {
            return false;
        }
        int newLevel = unitsToLevel(combinedUnits);
        serverLevel.setBlock(pos, ModFluids.BLIGHT_FLUID_BLOCK.get().defaultBlockState().setValue(LiquidBlock.LEVEL, newLevel), 3);
        return true;
    }
}

