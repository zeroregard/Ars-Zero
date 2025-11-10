package com.github.ars_zero.common.entity;

import com.github.ars_zero.registry.ModEntities;
import com.hollingsworth.arsnouveau.setup.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.tags.FluidTags;

public class WaterVoxelEntity extends BaseVoxelEntity {
    
    private static final int COLOR = 0x3F76E4;
    private static final float DEFAULT_BASE_SIZE = 0.25f;
    private static final float POTION_SHRINK_STEP = DEFAULT_BASE_SIZE / 2.0f;
    private static final int FARMLAND_CHECK_INTERVAL = 20;
    private static final int EVAPORATION_CHECK_INTERVAL = 20;
    
    private int farmlandTickCounter = 0;
    private int evaporationTickCounter = 0;
    private float casterWaterPower = 0.0f;
    private boolean forceHotEnvironment = false;
    
    public WaterVoxelEntity(EntityType<? extends WaterVoxelEntity> entityType, Level level) {
        super(entityType, level);
    }
    
    public WaterVoxelEntity(Level level, double x, double y, double z, int lifetime) {
        this(ModEntities.WATER_VOXEL_ENTITY.get(), level);
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
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.BUCKET)) {
            if (!player.getAbilities().instabuild) {
                ItemStack filled = ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET));
                player.setItemInHand(hand, filled);
            }
            this.discard();
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (stack.is(Items.GLASS_BOTTLE)) {
            if (!player.getAbilities().instabuild) {
                ItemStack filledBottle = new ItemStack(Items.POTION);
                filledBottle.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
                ItemStack filledResult = ItemUtils.createFilledResult(stack, player, filledBottle);
                player.setItemInHand(hand, filledResult);
            }
            shrinkForPotionFill();
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.interact(player, hand);
    }
    
    @Override
    public void tick() {
        if (!this.level().isClientSide && this.isAlive()) {
            if (handleLavaContact()) {
                super.tick();
                return;
            }
            boolean farmlandNearby = updateFarmlandHydration(false);
            if (farmlandNearby) {
                farmlandTickCounter++;
                if (farmlandTickCounter >= FARMLAND_CHECK_INTERVAL) {
                    farmlandTickCounter = 0;
                    updateFarmlandHydration(true);
                }
            } else {
                farmlandTickCounter = 0;
            }
            handleHotBiomeEvaporation();
        }
        super.tick();
    }
    
    @Override
    protected void onBlockCollision(BlockHitResult blockHit) {
        BlockPos targetPos = blockHit.getBlockPos();
        BlockState targetState = this.level().getBlockState(targetPos);
        if (targetState.getFluidState().is(FluidTags.LAVA)) {
            if (targetState.getFluidState().isSource()) {
                this.level().setBlock(targetPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
            } else {
                this.level().setBlock(targetPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
            return;
        }
        if (blockHit.getDirection() == Direction.UP) {
            BlockPos cauldronPos = targetPos;
            BlockState cauldronState = targetState;
            if (cauldronState.is(Blocks.CAULDRON)) {
                this.level().setBlock(
                    cauldronPos,
                    Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1),
                    3
                );
                return;
            }
            if (cauldronState.is(Blocks.WATER_CAULDRON)) {
                int currentLevel = cauldronState.getValue(LayeredCauldronBlock.LEVEL);
                if (currentLevel < LayeredCauldronBlock.MAX_FILL_LEVEL) {
                    this.level().setBlock(
                        cauldronPos,
                        cauldronState.setValue(LayeredCauldronBlock.LEVEL, currentLevel + 1),
                        3
                    );
                    return;
                }
            }
        }
        BlockPos pos = targetPos.relative(blockHit.getDirection());
        if (this.level().getBlockState(pos).isAir()) {
            int waterLevel = calculateWaterLevel();
            this.level().setBlock(pos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, waterLevel), 3);
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
            Vec3 hitLocation = result.getLocation();
            
            BlockPos centerPos = new BlockPos(
                (int) Math.round(hitLocation.x),
                (int) Math.round(hitLocation.y),
                (int) Math.round(hitLocation.z)
            );
            
            int waterLevel = calculateWaterLevel();
            for (BlockPos pos : BlockPos.betweenClosed(centerPos.offset(-1, -1, -1), centerPos.offset(1, 1, 1))) {
                if (this.level().getBlockState(pos).isAir()) {
                    this.level().setBlock(pos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, waterLevel), 3);
                    break;
                }
            }
        }
        this.discard();
    }
    
    private boolean updateFarmlandHydration(boolean hydrate) {
        BlockPos center = this.blockPosition();
        boolean farmlandFound = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos targetPos = center.offset(dx, -1, dz);
                BlockState targetState = this.level().getBlockState(targetPos);
                if (targetState.getBlock() instanceof FarmBlock) {
                    farmlandFound = true;
                    if (hydrate && targetState.hasProperty(FarmBlock.MOISTURE)) {
                        int currentMoisture = targetState.getValue(FarmBlock.MOISTURE);
                        if (currentMoisture < FarmBlock.MAX_MOISTURE) {
                            this.level().setBlock(
                                targetPos,
                                targetState.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE),
                                3
                            );
                        }
                    }
                }
            }
        }
        return farmlandFound;
    }
    
    private void shrinkForPotionFill() {
        if (!this.isAlive() || this.level().isClientSide) {
            return;
        }
        float newSize = this.getSize() - POTION_SHRINK_STEP;
        if (newSize <= 0.0f || newSize < 0.0625f) {
            this.discard();
            return;
        }
        this.setSize(newSize);
        this.refreshDimensions();
    }
    
    private void handleHotBiomeEvaporation() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            boolean ultraWarm = serverLevel.dimensionType().ultraWarm();
            float biomeTemperature = serverLevel.getBiome(this.blockPosition()).value().getBaseTemperature();
            boolean hot = ultraWarm || forceHotEnvironment || biomeTemperature > 1.0F;
            if (!hot) {
                evaporationTickCounter = 0;
                return;
            }
            if (ultraWarm) {
                this.discard();
                return;
            }
            evaporationTickCounter++;
            if (evaporationTickCounter >= EVAPORATION_CHECK_INTERVAL) {
                evaporationTickCounter = 0;
                float shrinkPercent = getEvaporationPercent();
                if (shrinkPercent <= 0.0f) {
                    return;
                }
                float newSize = this.getSize() * (1.0f - shrinkPercent);
                if (newSize <= 0.0f || newSize < 0.0625f) {
                    this.discard();
                    return;
                }
                this.setSize(newSize);
                this.refreshDimensions();
            }
        }
    }
    
    private float getEvaporationPercent() {
        if (casterWaterPower >= 2.0f) {
            return 0.0f;
        }
        if (casterWaterPower >= 1.0f) {
            return 0.025f;
        }
        return 0.05f;
    }
    
    private boolean handleLavaContact() {
        BlockPos currentPos = this.blockPosition();
        BlockState state = this.level().getBlockState(currentPos);
        if (state.getFluidState().is(FluidTags.LAVA)) {
            if (state.getFluidState().isSource()) {
                this.level().setBlock(currentPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
            } else {
                this.level().setBlock(currentPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
            this.discard();
            return true;
        }
        return false;
    }
    
    public void setCasterWaterPower(float waterPower) {
        this.casterWaterPower = Math.max(0.0f, waterPower);
    }
    
    public float getCasterWaterPower() {
        return this.casterWaterPower;
    }
    
    public void setForceHotEnvironment(boolean forceHotEnvironment) {
        this.forceHotEnvironment = forceHotEnvironment;
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("WaterPower")) {
            this.casterWaterPower = compound.getFloat("WaterPower");
        }
        if (compound.contains("EvaporationCounter")) {
            this.evaporationTickCounter = compound.getInt("EvaporationCounter");
        }
        if (compound.contains("ForceHotEnvironment")) {
            this.forceHotEnvironment = compound.getBoolean("ForceHotEnvironment");
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("WaterPower", this.casterWaterPower);
        compound.putInt("EvaporationCounter", this.evaporationTickCounter);
        if (this.forceHotEnvironment) {
            compound.putBoolean("ForceHotEnvironment", true);
        }
    }
    
    private int calculateWaterLevel() {
        float size = this.getSize();
        float ratio = size / 1.0f;
        
        if (ratio >= 1.0f) {
            return 0;
        } else if (ratio >= 0.875f) {
            return 1;
        } else if (ratio >= 0.75f) {
            return 2;
        } else if (ratio >= 0.625f) {
            return 3;
        } else if (ratio >= 0.5f) {
            return 4;
        } else if (ratio >= 0.375f) {
            return 5;
        } else if (ratio >= 0.25f) {
            return 6;
        } else {
            return 7;
        }
    }
    
    private int calculateParticleCount() {
        float size = this.getSize();
        float ratio = size / 1.0f;
        
        int baseCount = (int) (ratio * 32);
        return Math.min(baseCount, 32);
    }
    
    @Nullable
    protected SoundEvent getSpawnSound() {
        return SoundRegistry.TEMPESTRY_FAMILY.get();
    }
    
    @Override
    protected net.minecraft.core.particles.ParticleOptions getAmbientParticle() {
        return ParticleTypes.FALLING_WATER;
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
                    ParticleTypes.SPLASH,
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
                    ParticleTypes.BUBBLE_POP,
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
}

