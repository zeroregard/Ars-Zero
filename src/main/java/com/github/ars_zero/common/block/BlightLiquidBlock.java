package com.github.ars_zero.common.block;

import com.github.ars_zero.common.block.interaction.ConvertMossyInteraction;
import com.github.ars_zero.common.block.interaction.ConvertToDirtInteraction;
import com.github.ars_zero.common.block.interaction.DestroyFloraInteraction;
import com.github.ars_zero.registry.ModBlocks;
import com.github.ars_zero.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class BlightLiquidBlock extends LiquidBlock {
    
    private static final List<BlightInteraction> INTERACTIONS = List.of(
        new ConvertToDirtInteraction(),
        new ConvertMossyInteraction(),
        new DestroyFloraInteraction()
    );
    
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
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
        } else if (!level.isClientSide && entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            if (stack.getItem() instanceof BlockItem blockItem) {
                net.minecraft.world.level.block.Block converted = BlightItemConversions.convert(blockItem.getBlock());
                if (converted != null) {
                    itemEntity.setItem(new ItemStack(converted, stack.getCount()));
                    if (level instanceof ServerLevel serverLevel) {
                        playInteractionEffects(serverLevel, pos);
                    }
                    super.entityInside(state, level, pos, entity);
                    return;
                }
            }
            boolean shouldVaporize = stack.is(net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ars_zero", "blight_cauldron_vaporizes")
            ));
            if (!shouldVaporize && stack.getItem() instanceof BlockItem blockItem) {
                BlockState blockState = blockItem.getBlock().defaultBlockState();
                shouldVaporize = blockState.is(BlockTags.FLOWERS)
                    || blockState.is(BlockTags.SAPLINGS)
                    || blockState.is(BlockTags.LEAVES)
                    || blockState.is(net.minecraft.tags.TagKey.create(
                        net.minecraft.core.registries.Registries.BLOCK,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "replaceable_plants")
                    ));
            }
            if (shouldVaporize) {
                itemEntity.discard();
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ModParticles.BLIGHT_SPLASH.get(), pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, 10, 0.25, 0.15, 0.25, 0.0);
                }
            }
        }
        super.entityInside(state, level, pos, entity);
    }
    
    @Override
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (level.getFluidState(pos).isSource() && hasAdjacentIce(level, pos)) {
            level.setBlock(pos, ModBlocks.FROZEN_BLIGHT.get().defaultBlockState(), 3);
            return;
        }
        super.tick(state, level, pos, random);
        
        if (random.nextInt(8) == 0) {
            checkAndAffectAdjacentBlocks(level, pos);
        }
    }
    
    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            if (level.getFluidState(pos).isSource() && hasAdjacentIce(level, pos)) {
                level.setBlock(pos, ModBlocks.FROZEN_BLIGHT.get().defaultBlockState(), 3);
                return;
            }
            checkAndAffectAdjacentBlocks(serverLevel, pos);
        }
    }
    
    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull net.minecraft.world.level.block.Block neighborBlock, @NotNull BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            if (level.getFluidState(pos).isSource() && hasAdjacentIce(level, pos)) {
                level.setBlock(pos, ModBlocks.FROZEN_BLIGHT.get().defaultBlockState(), 3);
                return;
            }
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);
            for (BlightInteraction interaction : INTERACTIONS) {
                if (interaction.matches(belowState)) {
                    interaction.apply(serverLevel, belowPos, belowState);
                    playInteractionEffects(serverLevel, belowPos);
                    break;
                }
            }
        }
    }
    
    private void checkAndAffectAdjacentBlocks(ServerLevel level, BlockPos pos) {
        for (BlockPos target : targetsAround(pos)) {
            BlockState state = level.getBlockState(target);
            
            if (state.getBlock() == Blocks.WATER || state.getFluidState().is(FluidTags.WATER)) {
                continue;
            }
            
            for (BlightInteraction interaction : INTERACTIONS) {
                if (interaction.matches(state)) {
                    interaction.apply(level, target, state);
                    playInteractionEffects(level, target);
                    return;
                }
            }
        }
    }
    
    private Iterable<BlockPos> targetsAround(BlockPos pos) {
        return List.of(
            pos.below(),
            pos.above(),
            pos.north(),
            pos.south(),
            pos.east(),
            pos.west()
        );
    }
    
    private static boolean hasAdjacentIce(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(BlockTags.ICE)) {
                return true;
            }
        }
        return false;
    }
    
    public static BlockState copyProperties(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property<?> prop : from.getProperties()) {
            if (result.hasProperty(prop)) {
                result = copyProperty(from, result, prop);
            }
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<?> prop) {
        Property<T> typedProp = (Property<T>) prop;
        return to.setValue(typedProp, from.getValue(typedProp));
    }
    
    private void playInteractionEffects(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.0f);
            serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 
                    8, 0.2, 0.2, 0.2, 0.02);
        }
    }
}
