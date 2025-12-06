package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import com.github.ars_zero.registry.ModFluids;
import com.github.ars_zero.registry.ModParticles;
import com.github.ars_zero.common.block.BlightLiquidBlock;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
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
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class BlightVoxelEntity extends BaseVoxelEntity {
    private static final int COLOR = 0x6BFF78;
    private static final ResourceKey<net.minecraft.world.effect.MobEffect> ENVENOM_EFFECT_KEY =
        ResourceKey.create(Registries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath("ars_nouveau", "envenom"));
    private static final ResourceKey<net.minecraft.world.effect.MobEffect> POISON_EFFECT_KEY =
        ResourceKey.create(Registries.MOB_EFFECT, ResourceLocation.parse("minecraft:poison"));

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
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.isAlive()) {
            BlockPos currentPos = this.blockPosition();
            BlockState currentState = this.level().getBlockState(currentPos);
            if (currentState.getBlock() == Blocks.WATER || currentState.getFluidState().is(FluidTags.WATER)) {
                transformWaterToBlight(currentPos, currentState);
            } else {
                for (Direction dir : Direction.values()) {
                    BlockPos adjacentPos = currentPos.relative(dir);
                    BlockState adjacentState = this.level().getBlockState(adjacentPos);
                    if (adjacentState.getBlock() == Blocks.WATER || adjacentState.getFluidState().is(FluidTags.WATER)) {
                        transformWaterToBlight(adjacentPos, adjacentState);
                        break;
                    }
                }
            }
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
        this.discard();
    }

    @Override
    protected void onBlockCollision(BlockHitResult blockHit) {
        if (!this.level().isClientSide) {
            BlockPos hitPos = blockHit.getBlockPos();
            BlockState hitState = this.level().getBlockState(hitPos);
            
            if (hitState.getBlock() == Blocks.WATER || hitState.getFluidState().is(FluidTags.WATER)) {
                transformWaterToBlight(hitPos, hitState);
            } else if (isFlower(hitState)) {
                destroyFlowerAndPlaceLiquid(hitPos, hitState, blockHit);
            } else {
                BlockPos placePos = hitPos.relative(blockHit.getDirection());
                placeBlightFluidWithUnits(placePos);
            }
            spawnHitParticles(blockHit.getLocation());
        }
    }
    
    private boolean isFlower(BlockState state) {
        return state.is(BlockTags.FLOWERS);
    }
    
    private void destroyFlowerAndPlaceLiquid(BlockPos pos, BlockState flowerState, BlockHitResult blockHit) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.destroyBlock(pos, false);
        serverLevel.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.0f);
        BlockPos placePos = pos;
        placeBlightFluidWithUnits(placePos);
    }

    @Override
    protected void spawnHitParticles(Vec3 location) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < 8; i++) {
            double ox = (this.random.nextDouble() - 0.5D) * 0.3;
            double oy = (this.random.nextDouble() - 0.5D) * 0.3;
            double oz = (this.random.nextDouble() - 0.5D) * 0.3;
            serverLevel.sendParticles(ModParticles.BLIGHT_SPLASH.get(), location.x + ox, location.y + oy, location.z + oz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
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

    private void transformWaterToBlight(BlockPos pos, BlockState waterState) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int waterLevel = 0;
        if (waterState.getBlock() == Blocks.WATER) {
            waterLevel = waterState.getValue(LiquidBlock.LEVEL);
        }
        int blightLevel = waterLevel;
        serverLevel.setBlock(pos, ModFluids.BLIGHT_FLUID_BLOCK.get().defaultBlockState().setValue(LiquidBlock.LEVEL, blightLevel), 3);
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

