package com.github.ars_zero.common.block;

import com.github.ars_zero.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class BlightLiquidBlock extends LiquidBlock {
    
    public BlightLiquidBlock(Supplier<? extends FlowingFluid> fluidSupplier, Properties properties) {
        super(fluidSupplier.get(), properties);
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }
    
    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        }
        super.entityInside(state, level, pos, entity);
    }
    
    @Override
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        super.tick(state, level, pos, random);
        checkAndAffectAdjacentBlocks(level, pos);
    }
    
    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            checkAndAffectAdjacentBlocks(serverLevel, pos);
        }
    }
    
    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block neighborBlock, @NotNull BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);
            if (shouldConvertToDirt(belowState)) {
                serverLevel.setBlock(belowPos, Blocks.DIRT.defaultBlockState(), 3);
                playInteractionEffects(serverLevel, belowPos);
            }
        }
    }
    
    private void checkAndAffectAdjacentBlocks(ServerLevel level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        
        if (shouldConvertToDirt(belowState)) {
            level.setBlock(belowPos, Blocks.DIRT.defaultBlockState(), 3);
            playInteractionEffects(level, belowPos);
            return;
        }
        if (shouldConvertWaterToBlight(belowState)) {
            convertWaterToBlight(level, belowPos, belowState);
            playInteractionEffects(level, belowPos);
            return;
        }
        
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        
        if (shouldDestroyFlora(aboveState)) {
            level.destroyBlock(abovePos, false);
            playInteractionEffects(level, abovePos);
            return;
        }
        if (shouldConvertWaterToBlight(aboveState)) {
            convertWaterToBlight(level, abovePos, aboveState);
            playInteractionEffects(level, abovePos);
            return;
        }
        
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            
            if (shouldConvertToDirt(adjacentState)) {
                level.setBlock(adjacentPos, Blocks.DIRT.defaultBlockState(), 3);
                playInteractionEffects(level, adjacentPos);
                return;
            }
            if (shouldDestroyFlora(adjacentState)) {
                level.destroyBlock(adjacentPos, false);
                playInteractionEffects(level, adjacentPos);
                return;
            }
            if (shouldConvertWaterToBlight(adjacentState)) {
                convertWaterToBlight(level, adjacentPos, adjacentState);
                playInteractionEffects(level, adjacentPos);
                return;
            }
        }
    }
    
    private boolean shouldDestroyFlora(BlockState state) {
        return state.is(BlockTags.LEAVES) || 
               state.is(BlockTags.FLOWERS) ||
               state.is(BlockTags.SAPLINGS) ||
               state.getBlock() == Blocks.SHORT_GRASS ||
               state.getBlock() == Blocks.TALL_GRASS ||
               state.getBlock() == Blocks.FERN ||
               state.getBlock() == Blocks.LARGE_FERN ||
               state.getBlock() == Blocks.DEAD_BUSH ||
               state.getBlock() == Blocks.VINE ||
               state.getBlock() == Blocks.GLOW_LICHEN;
    }
    
    private boolean shouldConvertToDirt(BlockState state) {
        return state.getBlock() == Blocks.GRASS_BLOCK ||
               state.getBlock() == Blocks.MYCELIUM ||
               state.getBlock() == Blocks.PODZOL ||
               state.getBlock() == Blocks.COARSE_DIRT ||
               state.getBlock() == Blocks.FARMLAND ||
               state.getBlock() == Blocks.MOSS_BLOCK;
    }
    
    private boolean shouldConvertWaterToBlight(BlockState state) {
        return state.getBlock() == Blocks.WATER || state.getFluidState().is(FluidTags.WATER);
    }
    
    private void convertWaterToBlight(ServerLevel level, BlockPos pos, BlockState waterState) {
        if (waterState.getBlock() == Blocks.WATER) {
            int waterLevel = waterState.getValue(LiquidBlock.LEVEL);
            BlockState blightState = ModFluids.BLIGHT_FLUID_BLOCK.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, waterLevel);
            level.setBlock(pos, blightState, 3);
        } else {
            FluidState fluidState = waterState.getFluidState();
            if (fluidState.isSource()) {
                level.setBlock(pos, ModFluids.BLIGHT_FLUID_BLOCK.get().defaultBlockState(), 3);
            } else {
                int amount = fluidState.getAmount();
                int levelValue = Math.max(0, Math.min(7, 8 - amount));
                BlockState blightState = ModFluids.BLIGHT_FLUID_BLOCK.get().defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, levelValue);
                level.setBlock(pos, blightState, 3);
            }
        }
        level.scheduleTick(pos, ModFluids.BLIGHT_FLUID_BLOCK.get(), ModFluids.BLIGHT_FLUID_FLOWING.get().getTickDelay(level));
    }
    
    private void playInteractionEffects(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.0f);
            serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 
                    8, 0.2, 0.2, 0.2, 0.02);
        }
    }
}
